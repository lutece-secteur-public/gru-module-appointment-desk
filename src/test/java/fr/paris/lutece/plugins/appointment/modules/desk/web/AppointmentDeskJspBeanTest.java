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

import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.appointment.web.AppointmentFormJspBean;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.test.AdminUserUtils;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import fr.paris.lutece.test.mocks.MockHttpServletResponse;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.weld.proxy.WeldClientProxy;

/**
 * Opening and closing desks from the desk screen
 */
public class AppointmentDeskJspBeanTest extends LuteceTestCase
{
    /**
     * Slots the action cannot read: back to the desk screen of the form, on the same day, which shows the error
     *
     * @throws Exception
     *             if the controller cannot be initialized
     */
    @Test
    public void testCloseDeskWithUnreadableSlotsRedirectsToTheScreen( ) throws Exception
    {
        AppointmentDeskJspBean instance = CDI.current( ).select( AppointmentDeskJspBean.class ).get( );
        RedirectResponse response = new RedirectResponse( );

        // processController gives the bean its response: set it as processController does
        Field fieldResponse = MVCAdminJspBean.class.getDeclaredField( "_response" );
        fieldResponse.setAccessible( true );
        fieldResponse.set( ( (WeldClientProxy) instance ).getMetadata( ).getContextualInstance( ), response );

        MockHttpServletRequest request = new MockHttpServletRequest( );
        AdminUserUtils.registerAdminUserWithRight( request, new AdminUser( ), AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM );
        instance.init( request, AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM );
        request.addParameter( "data", "not a list of slots" );
        request.addParameter( "id_form", "1" );
        request.addParameter( "day", "2026-11-10" );
        request.addParameter( "context", "desk" );
        String strContent = instance.docloseAppointmentDesk( request );

        String strLocation = response._strLocation;
        assertTrue( "No redirect, content: " + strContent, strLocation != null );
        assertTrue( strLocation, strLocation.contains( "view=manageAppointmentDesks" ) );
        assertTrue( strLocation, strLocation.contains( "id_form=1" ) );
        assertTrue( strLocation, strLocation.contains( "day=2026-11-10" ) );
    }

    /**
     * Response that keeps the location of the redirect
     */
    private static class RedirectResponse extends MockHttpServletResponse
    {
        private String _strLocation;

        @Override
        public void sendRedirect( String strLocation ) throws IOException
        {
            _strLocation = strLocation;
        }
    }
}
