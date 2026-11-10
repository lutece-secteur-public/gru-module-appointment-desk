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

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.appointment.business.form.Form;
import fr.paris.lutece.plugins.appointment.business.form.FormHome;
import fr.paris.lutece.plugins.appointment.web.AppointmentFormJspBean;
import fr.paris.lutece.portal.business.rbac.RBACRole;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
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

    @AfterEach
    public void removeForm( )
    {
        if ( _nIdForm != -1 )
        {
            FormHome.delete( _nIdForm );
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
