/*
 * Copyright (c) 2002-2022, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.appointment.modules.desk.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.appointment.business.planning.ClosingDay;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.modules.desk.util.AppointmentDeskPlugin;
import fr.paris.lutece.plugins.appointment.modules.desk.util.IncrementSlot;
import fr.paris.lutece.plugins.appointment.modules.desk.util.IncrementingType;
import fr.paris.lutece.plugins.appointment.service.ClosingDayService;
import fr.paris.lutece.plugins.appointment.service.SlotSafeService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.sql.TransactionManager;

public class AppointmentDeskService
{

    /**
     * Utility class.
     */
    private AppointmentDeskService( )
    {
    }

    /**
     * Close one desk on each slot: the capacity of the slot, as stored, loses one place. A slot of the typical week not
     * stored yet is created first.
     * 
     * @param listSlot
     *            the slots, as the desk screen sends them
     */
    public static void closeAppointmentDesk( List<Slot> listSlot )
    {
        for ( Slot slot : listSlot )
        {
            changeCapacity( storedSlot( slot ), -1, Integer.MAX_VALUE );
        }
    }

    /**
     * Open one desk on each slot: the capacity of the slot, as stored, gains one place, up to the number of desks of the
     * day. Nothing is opened on a closing day.
     * 
     * @param listSlot
     *            the slots, as the desk screen sends them
     * @param nMaxCapacity
     *            the number of desks of the day
     */
    public static void openAppointmentDesk( List<Slot> listSlot, int nMaxCapacity )
    {
        if ( listSlot.isEmpty( ) || ClosingDayService.findClosingDayByIdFormAndDateOfClosingDay( listSlot.get( 0 ).getIdForm( ),
                listSlot.get( 0 ).getStartingDateTime( ).toLocalDate( ) ) != null )
        {
            return;
        }
        for ( Slot slot : listSlot )
        {
            changeCapacity( storedSlot( slot ), 1, nMaxCapacity );
        }
    }

    /**
     * Get the id of the stored slot matching a slot sent by the desk screen, creating it when it only exists in the
     * typical week.
     * 
     * @param slot
     *            the slot sent by the screen
     * @return the id of the stored slot
     */
    private static int storedSlot( Slot slot )
    {
        if ( slot.getIdSlot( ) != 0 )
        {
            return slot.getIdSlot( );
        }
        SlotService.addDateAndTimeToSlot( slot );
        slot.setNbRemainingPlaces( slot.getMaxCapacity( ) );
        slot.setNbPotentialRemainingPlaces( slot.getMaxCapacity( ) );
        return SlotSafeService.saveSlot( slot ).getIdSlot( );
    }

    /**
     * Add a number of places to the capacity of a stored slot, under its lock, keeping the capacity between 0 and a
     * maximum. Only the capacity and the remaining places change: every other value is the stored one.
     * 
     * @param nIdSlot
     *            the id of the stored slot
     * @param nDelta
     *            the places to add, negative to remove
     * @param nMaxCapacity
     *            the highest capacity allowed
     */
    private static void changeCapacity( int nIdSlot, int nDelta, int nMaxCapacity )
    {
        Lock lock = SlotSafeService.getLockOnSlot( nIdSlot );
        lock.lock( );
        try
        {
            Slot slot = SlotService.findSlotById( nIdSlot );
            int nCapacity = ( slot != null ) ? slot.getMaxCapacity( ) + nDelta : -1;
            if ( nCapacity < 0 || nCapacity > nMaxCapacity )
            {
                return;
            }
            slot.setMaxCapacity( nCapacity );
            slot.setNbPotentialRemainingPlaces( slot.getNbPotentialRemainingPlaces( ) + nDelta );
            slot.setNbRemainingPlaces( slot.getNbRemainingPlaces( ) + nDelta );
            if ( nDelta > 0 )
            {
                slot.setIsOpen( true );
            }
            slot.setIsSpecific( SlotService.isSpecificSlot( slot ) );
            TransactionManager.beginTransaction( AppointmentDeskPlugin.getPlugin( ) );
            try
            {
                SlotSafeService.saveSlot( slot );
                TransactionManager.commitTransaction( AppointmentDeskPlugin.getPlugin( ) );
            }
            catch( Exception e )
            {
                TransactionManager.rollBack( AppointmentDeskPlugin.getPlugin( ) );
                AppLogService.error( "Error changing the capacity of the slot {}", nIdSlot, e );
            }
        }
        finally
        {
            lock.unlock( );
        }
    }

    /**
     * Add places to every slot of a form between two dates, for the whole day, a half day, or a time range.
     * 
     * @param incrementSlot
     *            the places to add, the dates, the times and the kind of increment
     */
    public static void incrementMaxCapacity( IncrementSlot incrementSlot )
    {

        LocalDateTime startingDateTimes;
        LocalDateTime endingDateTimes;
        boolean lace = false;

        if ( StringUtils.isNotEmpty( incrementSlot.getStartingTime( ) )
                && IncrementingType.HALFTIMEMORNING.getValue( ) != incrementSlot.getType( ).getValue( ) )
        {
            startingDateTimes = incrementSlot.getStartingDate( ).atTime( LocalTime.parse( incrementSlot.getStartingTime( ) ) );
        }
        else
        {
            startingDateTimes = incrementSlot.getStartingDate( ).atStartOfDay( );
        }

        if ( StringUtils.isNotEmpty( incrementSlot.getEndingTime( ) )
                && IncrementingType.HALFTIMEAFTERNOON.getValue( ) != incrementSlot.getType( ).getValue( ) )
        {
            endingDateTimes = incrementSlot.getEndingDate( ).atTime( LocalTime.parse( incrementSlot.getEndingTime( ) ) );
        }
        else
        {
            endingDateTimes = incrementSlot.getEndingDate( ).atTime( LocalTime.MAX );
        }

        if ( incrementSlot.getType( ).getValue( ) == IncrementingType.LACE.getValue( ) )
        {
            lace = true;
        }

        SlotSafeService.incrementMaxCapacity( incrementSlot.getIdForm( ), incrementSlot.getIncrementingValue( ), startingDateTimes, endingDateTimes, lace );
    }

}
