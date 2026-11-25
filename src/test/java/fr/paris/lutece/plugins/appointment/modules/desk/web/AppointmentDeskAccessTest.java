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
package fr.paris.lutece.plugins.appointment.modules.desk.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.appointment.business.calendar.CalendarTemplate;
import fr.paris.lutece.plugins.appointment.business.calendar.CalendarTemplateHome;
import fr.paris.lutece.plugins.appointment.business.form.Form;
import fr.paris.lutece.plugins.appointment.business.form.FormHome;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.web.AppointmentFormJspBean;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.portal.business.rbac.RBACRole;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.image.ImageResource;
import fr.paris.lutece.test.AdminUserUtils;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * The desks of a day show the appointments of the form: the user must be allowed to see the form
 */
public class AppointmentDeskAccessTest extends LuteceTestCase
{
    private static final String ROLE_SUPER_ADMIN = "super_admin";

    private int _nIdForm = -1;
    private int _nIdFormDto = -1;

    @AfterEach
    public void removeForm( )
    {
        if ( _nIdForm != -1 )
        {
            FormHome.delete( _nIdForm );
        }
        if ( _nIdFormDto != -1 )
        {
            FormService.removeForm( _nIdFormDto );
        }
    }

    /**
     * A user without any permission on the form
     */
    @Test
    public void testDayIsRefusedWithoutPermission( ) throws Exception
    {
        _nIdForm = createForm( null );

        assertRefused( new AdminUser( ) );
    }

    /**
     * A user outside the workgroup of the form, even with every permission
     */
    @Test
    public void testDayIsRefusedOutsideTheWorkgroup( ) throws Exception
    {
        _nIdForm = createForm( "desk_access_test_wg" );
        AdminUser user = new AdminUser( );
        user.setRoles( Map.of( ROLE_SUPER_ADMIN, new RBACRole( ROLE_SUPER_ADMIN, ROLE_SUPER_ADMIN ) ) );

        assertRefused( user );
    }

    /**
     * Adding a desk on a form outside the workgroups of the user is refused, as its day view is, even with every permission:
     * the capacity of the form is not changed
     */
    @Test
    public void testIncrementIsRefusedOutsideTheWorkgroup( ) throws Exception
    {
        _nIdFormDto = createFormWithTypicalWeek( "desk_access_test_wg" );
        LocalDate day = LocalDate.now( ).plusDays( 3 );
        AdminUser user = new AdminUser( );
        user.setRoles( Map.of( ROLE_SUPER_ADMIN, new RBACRole( ROLE_SUPER_ADMIN, ROLE_SUPER_ADMIN ) ) );
        MockHttpServletRequest request = new MockHttpServletRequest( );
        AdminUserUtils.registerAdminUserWithRight( request, user, AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM );
        AppointmentDeskJspBean instance = CDI.current( ).select( AppointmentDeskJspBean.class ).get( );
        instance.init( request, AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM );
        String strDay = day.format( DateTimeFormatter.ofPattern( "dd/MM/yyyy" ) );
        request.addParameter( "id_form", String.valueOf( _nIdFormDto ) );
        request.addParameter( "incrementing_value", "1" );
        request.addParameter( "type", "4" );
        request.addParameter( "starting_date", strDay );
        request.addParameter( "ending_date", strDay );

        assertThrows( AccessDeniedException.class, ( ) -> instance.doIncrementMaxCapacity( request ) );
        assertTrue( SlotService.findSlotsByIdFormAndDateRange( _nIdFormDto, day.atStartOfDay( ), day.atTime( 23, 59 ) ).isEmpty( ),
                "no slot of the form changed" );
    }

    private static int createFormWithTypicalWeek( String strWorkgroup )
    {
        CalendarTemplate calendarTemplate = new CalendarTemplate( );
        calendarTemplate.setTitle( "desk access test" );
        calendarTemplate.setDescription( "desk access test" );
        calendarTemplate.setTemplatePath( "desk_access_test.html" );
        CalendarTemplateHome.create( calendarTemplate );
        AppointmentFormDTO form = new AppointmentFormDTO( );
        form.setTitle( "desk access test" );
        form.setName( "desk access test" );
        form.setDescription( "desk access test" );
        form.setDescriptionRule( "desk access test" );
        form.setWorkgroup( strWorkgroup );
        form.setTimeStart( "09:00" );
        form.setTimeEnd( "10:00" );
        form.setDurationAppointments( 30 );
        form.setDateStartValidity( Date.valueOf( LocalDate.now( ) ) );
        form.setDateEndValidity( Date.valueOf( LocalDate.now( ).plusMonths( 1 ) ) );
        form.setNbWeeksToDisplay( 3 );
        form.setMaxPeoplePerAppointment( 1 );
        form.setMaxCapacityPerSlot( 1 );
        form.setIsOpenMonday( true );
        form.setIsOpenTuesday( true );
        form.setIsOpenWednesday( true );
        form.setIsOpenThursday( true );
        form.setIsOpenFriday( true );
        form.setIsOpenSaturday( true );
        form.setIsOpenSunday( true );
        form.setCalendarTemplateId( calendarTemplate.getIdCalendarTemplate( ) );
        form.setIsActive( true );
        ImageResource icon = new ImageResource( );
        icon.setImage( new byte [ 0] );
        icon.setMimeType( "image/png" );
        form.setIcon( icon );
        return FormService.createAppointmentForm( form );
    }

    private void assertRefused( AdminUser user ) throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        AdminUserUtils.registerAdminUserWithRight( request, user, AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM );
        AppointmentDeskJspBean instance = CDI.current( ).select( AppointmentDeskJspBean.class ).get( );
        instance.init( request, AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM );
        request.addParameter( "id_form", String.valueOf( _nIdForm ) );
        request.addParameter( "day", "2026-11-10" );

        assertThrows( AccessDeniedException.class, ( ) -> instance.getManageAppointmentDesks( request ) );
    }

    private static int createForm( String strWorkgroup )
    {
        Form form = new Form( );
        form.setTitle( "Desk access test" );
        form.setDescription( "Desk access test" );
        form.setWorkgroup( strWorkgroup );
        return FormHome.create( form ).getIdForm( );
    }
}
