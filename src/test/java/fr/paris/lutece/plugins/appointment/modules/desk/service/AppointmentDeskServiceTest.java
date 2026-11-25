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

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.appointment.business.calendar.CalendarTemplate;
import fr.paris.lutece.plugins.appointment.business.calendar.CalendarTemplateHome;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.business.slot.SlotHome;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.service.WeekDefinitionService;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.portal.service.image.ImageResource;
import fr.paris.lutece.test.LuteceTestCase;

/**
 * Opening and closing desks, as the desk screen sends the slots: the capacity the screen sends is already the new one.
 */
public class AppointmentDeskServiceTest extends LuteceTestCase
{
    private static final int DESKS = 3;
    private int _nIdForm;
    private int _nIdCalendarTemplate;
    private LocalDate _day;

    @BeforeEach
    @Override
    protected void setUp( ) throws Exception
    {
        super.setUp( );
        CalendarTemplate calendarTemplate = new CalendarTemplate( );
        calendarTemplate.setTitle( "desk test" );
        calendarTemplate.setDescription( "desk test" );
        calendarTemplate.setTemplatePath( "desk_test.html" );
        CalendarTemplateHome.create( calendarTemplate );
        _nIdCalendarTemplate = calendarTemplate.getIdCalendarTemplate( );

        AppointmentFormDTO form = new AppointmentFormDTO( );
        form.setTitle( "desk test" );
        form.setName( "desk test" );
        form.setDescriptionRule( "desk test" );
        form.setDescription( "desk test" );
        form.setTimeStart( "09:00" );
        form.setTimeEnd( "12:00" );
        form.setDurationAppointments( 30 );
        form.setDateStartValidity( Date.valueOf( LocalDate.now( ) ) );
        form.setDateEndValidity( Date.valueOf( LocalDate.now( ).plusMonths( 1 ) ) );
        form.setNbWeeksToDisplay( 3 );
        form.setMaxPeoplePerAppointment( 1 );
        form.setMaxCapacityPerSlot( DESKS );
        form.setIsOpenMonday( true );
        form.setIsOpenTuesday( true );
        form.setIsOpenWednesday( true );
        form.setIsOpenThursday( true );
        form.setIsOpenFriday( true );
        form.setIsOpenSaturday( true );
        form.setIsOpenSunday( true );
        form.setCalendarTemplateId( _nIdCalendarTemplate );
        form.setIsActive( true );
        ImageResource icon = new ImageResource( );
        icon.setImage( new byte [ 0] );
        icon.setMimeType( "image/png" );
        form.setIcon( icon );
        _nIdForm = FormService.createAppointmentForm( form );
        _day = LocalDate.now( ).plusDays( 3 );
    }

    @AfterEach
    @Override
    protected void tearDown( ) throws Exception
    {
        if ( _nIdForm > 0 )
        {
            FormService.removeForm( _nIdForm );
        }
        CalendarTemplateHome.delete( _nIdCalendarTemplate );
        super.tearDown( );
    }

    /**
     * Closing one desk on a slot of the typical week never stored: the capacity loses one place, not two.
     */
    @Test
    public void testCloseOneDeskOnASlotNeverStored( )
    {
        Slot computed = slotAt( 10 );
        assertEquals( 0, computed.getIdSlot( ) );
        assertEquals( DESKS, computed.getMaxCapacity( ) );

        AppointmentDeskService.closeAppointmentDesk( List.of( sentByTheScreen( computed, DESKS - 1 ) ) );

        assertEquals( DESKS - 1, slotAt( 10 ).getMaxCapacity( ) );
    }

    /**
     * Once every desk of the day is closed, a desk can be opened again: the number of desks of the day is the one of the
     * typical week, not the highest capacity left.
     */
    @Test
    public void testOpenADeskOnADayWhoseDesksAreAllClosed( )
    {
        for ( int i = DESKS; i > 0; i-- )
        {
            for ( Slot slot : daySlots( ) )
            {
                AppointmentDeskService.closeAppointmentDesk( List.of( sentByTheScreen( slot, slot.getMaxCapacity( ) - 1 ) ) );
            }
        }
        assertTrue( daySlots( ).stream( ).allMatch( s -> s.getMaxCapacity( ) == 0 ) );
        assertEquals( DESKS, AppointmentDeskService.getNumberOfDesks( _nIdForm, _day ) );

        Slot slot = slotAt( 10 );
        AppointmentDeskService.openAppointmentDesk( List.of( sentByTheScreen( slot, 1 ) ) );

        assertEquals( 1, slotAt( 10 ).getMaxCapacity( ) );
    }

    /**
     * A booking made after the desk screen read the slots: the desk is not removed, the slot does not go below zero free
     * places
     */
    @Test
    public void testRemoveADeskBookedMeanwhile( )
    {
        Slot slot = slotAt( 10 );
        AppointmentDeskService.openAppointmentDesk( List.of( sentByTheScreen( slot, DESKS + 1 ) ), DESKS + 1 );
        setRemainingPlaces( slotAt( 10 ).getIdSlot( ), 1 );
        // the slots read by the screen, then a booking takes the last free place
        List<Slot> listReadByTheScreen = daySlots( );
        setRemainingPlaces( slotAt( 10 ).getIdSlot( ), 0 );

        assertFalse( AppointmentDeskService.removeAppointmentDesk( listReadByTheScreen, DESKS + 1 ) );
        Slot stored = SlotService.findSlotById( slotAt( 10 ).getIdSlot( ) );
        assertEquals( DESKS + 1, stored.getMaxCapacity( ) );
        assertEquals( 0, stored.getNbRemainingPlaces( ) );
    }

    /**
     * A desk number above the capacity of every slot of the day removes nothing and is not reported as removed
     */
    @Test
    public void testRemoveADeskThatDoesNotExist( )
    {
        Slot slot = slotAt( 10 );
        AppointmentDeskService.openAppointmentDesk( List.of( sentByTheScreen( slot, DESKS + 1 ) ), DESKS + 1 );

        assertFalse( AppointmentDeskService.removeAppointmentDesk( daySlots( ), DESKS + 5 ) );
        assertEquals( DESKS + 1, slotAt( 10 ).getMaxCapacity( ) );
    }

    private static void setRemainingPlaces( int nIdSlot, int nPlaces )
    {
        Slot slot = SlotService.findSlotById( nIdSlot );
        slot.setNbRemainingPlaces( nPlaces );
        slot.setNbPotentialRemainingPlaces( nPlaces );
        SlotHome.update( slot );
    }

    private List<Slot> daySlots( )
    {
        return SlotService.buildListSlot( _nIdForm, WeekDefinitionService.findAllWeekDefinition( _nIdForm ), _day, _day );
    }

    private Slot slotAt( int nHour )
    {
        LocalDateTime start = _day.atTime( nHour, 0 );
        return daySlots( ).stream( ).filter( s -> start.equals( s.getStartingDateTime( ) ) ).findFirst( ).orElseThrow( );
    }

    /**
     * The slot as the desk screen sends it: its id (0 when not stored), its times, and the new capacity.
     */
    private Slot sentByTheScreen( Slot slot, int nNewCapacity )
    {
        Slot sent = new Slot( );
        sent.setIdSlot( slot.getIdSlot( ) );
        sent.setIdForm( _nIdForm );
        sent.setStartingDateTime( slot.getStartingDateTime( ) );
        sent.setEndingDateTime( slot.getEndingDateTime( ) );
        sent.setMaxCapacity( nNewCapacity );
        sent.setIsOpen( nNewCapacity > 0 );
        return sent;
    }
}
