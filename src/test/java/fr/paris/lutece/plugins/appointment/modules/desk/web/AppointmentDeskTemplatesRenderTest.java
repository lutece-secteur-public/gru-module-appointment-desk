/*
 * Copyright (c) 2002-2026, City of Paris
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.portal.service.editor.RichTextContentMethod;
import fr.paris.lutece.portal.service.template.HtmlAttributesMethod;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.util.ReferenceList;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModelEx;

/**
 * Renders the desk screen with the configuration of the core (LUT-33153): HTML output format, auto-escaping, the back
 * office commons of the exploded webapp and htmlAttributes strict. The values written in the script are escaped for
 * JavaScript, the comments are sanitized rich text, the attributes of the buttons come from params maps. The macros of
 * plugin-appointment and the editor are stubbed.
 */
public class AppointmentDeskTemplatesRenderTest extends LuteceTestCase
{
    private static final String TEMPLATES_ROOT = "webapp/WEB-INF/templates";
    private static final String CORE_TEMPLATES_ROOT = "target/lutece/WEB-INF/templates";
    private static final String COMMONS_BO = "commons_bs5_tabler.html";
    private static final String TEMPLATE_MANAGE_DESKS = "admin/plugins/appointment/modules/desk/manage_appointmentdesks.html";

    /** Stand-ins for the macros of plugin-appointment and of the editor: they write their nested content. */
    private static final String MACROS_APPOINTMENT = "appointment_desk_test_macros.ftl";
    private static final String MACROS_APPOINTMENT_SOURCE = "<#macro appointmentTabs rest...><#nested></#macro>"
            + "<#macro getCommentModal rest...><div class=\"comment-modal\"></div></#macro>";
    private static final String EDITOR = "admin/util/editor/editor.html";
    private static final String EDITOR_SOURCE = "<#macro initEditor rest...></#macro>";

    private static final String NAME = "O'Hara \"Jo\" </script><b>x</b>";
    private static final String COMMENT = "<p>Desk <strong>closed</strong></p><img src=\"x\" onerror=\"alert(1)\">";

    /**
     * Renders a template of the module.
     *
     * @param strTemplate
     *            the template path
     * @param model
     *            the model
     * @return the HTML
     * @throws Exception
     *             if the rendering fails
     */
    private static String render( String strTemplate, Map<String, Object> model ) throws Exception
    {
        File coreTemplates = new File( CORE_TEMPLATES_ROOT );
        assertTrue( new File( coreTemplates, COMMONS_BO ).isFile( ), "core templates not found under " + CORE_TEMPLATES_ROOT + ": run lutece:exploded first" );
        StringTemplateLoader stubs = new StringTemplateLoader( );
        stubs.putTemplate( MACROS_APPOINTMENT, MACROS_APPOINTMENT_SOURCE );
        stubs.putTemplate( EDITOR, EDITOR_SOURCE );
        Configuration cfg = new Configuration( Configuration.VERSION_2_3_0 );
        cfg.setOutputFormat( HTMLOutputFormat.INSTANCE );
        cfg.setAutoEscapingPolicy( Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY );
        cfg.setURLEscapingCharset( "UTF-8" );
        cfg.setTemplateLoader( new MultiTemplateLoader( new TemplateLoader [ ] {
                stubs, new FileTemplateLoader( new File( TEMPLATES_ROOT ) ), new FileTemplateLoader( coreTemplates )
        } ) );
        cfg.addAutoInclude( COMMONS_BO );
        cfg.addAutoInclude( MACROS_APPOINTMENT );
        cfg.setSharedVariable( "htmlAttributes", new HtmlAttributesMethod( true ) );
        cfg.setSharedVariable( "i18n", (TemplateMethodModelEx) arguments -> String.valueOf( arguments.get( 0 ) ) );
        cfg.setSharedVariable( "dskey", (TemplateMethodModelEx) arguments -> "DS" );
        cfg.setSharedVariable( "richText", new RichTextContentMethod( ) );
        StringWriter writer = new StringWriter( );
        cfg.getTemplate( strTemplate ).process( model, writer );

        return writer.toString( );
    }

    /**
     * The model AppointmentDeskJspBean builds for one day: one slot, one appointment and one comment.
     *
     * @return the model
     */
    private static Map<String, Object> model( )
    {
        Map<String, Object> slot = new HashMap<>( );
        slot.put( "idSlot", 1234 );
        slot.put( "idForm", 1001 );
        slot.put( "startingDateTime", "2026-11-10T09:00" );
        slot.put( "endingDateTime", "2026-11-10T09:30" );
        slot.put( "isOpen", true );
        slot.put( "maxCapacity", 2 );
        slot.put( "isSpecific", false );
        Map<String, Object> user = new HashMap<>( );
        user.put( "lastName", NAME );
        user.put( "firstName", "Ann" );
        Map<String, Object> appointment = new HashMap<>( );
        appointment.put( "idAppointment", 5678 );
        appointment.put( "user", user );
        appointment.put( "nbPlaces", 1 );
        appointment.put( "appointmentTakenSqlDate", "2026-11-01" );
        appointment.put( "slot", List.of( slot ) );
        Map<String, Object> comment = new HashMap<>( );
        comment.put( "id", 7 );
        comment.put( "comment", COMMENT );
        comment.put( "creatorUserName", NAME );
        comment.put( "startingValidityDate", "2026-11-10" );
        comment.put( "endingValidityDate", "2026-11-10" );
        Map<String, Object> form = new HashMap<>( );
        form.put( "isMultislotAppointment", false );
        form.put( "durationAppointments", 30 );
        Map<String, Object> model = new HashMap<>( );
        model.put( "errors", List.of( ) );
        model.put( "warnings", List.of( ) );
        model.put( "infos", List.of( ) );
        model.put( "activateEditMode", true );
        model.put( "context", NAME );
        model.put( "list_comments", List.of( comment ) );
        model.put( "numb_desk", 2 );
        model.put( "list_slot", List.of( slot ) );
        model.put( "list_appointment", List.of( appointment ) );
        model.put( "language", "fr" );
        model.put( "day", "10/11/2026" );
        model.put( "idForm", 1001 );
        model.put( "appointmentForm", form );
        model.put( "locale", "fr" );
        model.put( "list_types", new ReferenceList( ) );
        model.put( "mailing_list", new ReferenceList( ) );
        model.put( "permission_add_comment", "true" );
        model.put( "permission_moderate_comment", "false" );
        model.put( "permission_access_code", NAME );
        model.put( "token_open_desk", "T1" );
        model.put( "token_close_desk", "T2" );

        return model;
    }

    /**
     * The values written in the script are escaped for JavaScript, not for HTML: a quote cannot end the string, the
     * script element cannot be closed, and no entity is left in the data. The numbers are written without grouping, the
     * messages through i18n().
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testScriptValuesAreEscapedForJavaScript( ) throws Exception
    {
        String strHtml = render( TEMPLATE_MANAGE_DESKS, model( ) );

        assertTrue( strHtml.contains( "\"lastName\":\"O\\'Hara \\\"Jo\\\" <\\/script>" ), strHtml );
        assertTrue( strHtml.contains( "const context = 'O\\'Hara \\\"Jo\\\" <\\/script>" ), strHtml );
        assertTrue( strHtml.contains( "ep.author === 'O\\'Hara" ), strHtml );
        assertFalse( strHtml.contains( "O&#39;Hara" ), strHtml );
        assertFalse( strHtml.contains( "&quot;Jo" ), strHtml );
        assertTrue( strHtml.contains( "\"id\":5678" ), strHtml );
        assertTrue( strHtml.contains( "idSlot: 1234," ), strHtml );
        assertTrue( strHtml.contains( "const idForm = '1001';" ), strHtml );
        assertTrue( strHtml.contains( "title: 'module.appointment.desk.label.surbooking'" ), strHtml );
        assertTrue( strHtml.contains( "const isCommentModerator = false;" ), strHtml );
    }

    /**
     * The comment is rich text: its tags are kept for the calendar, the event handler is removed.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testCommentIsSanitizedRichText( ) throws Exception
    {
        String strHtml = render( TEMPLATE_MANAGE_DESKS, model( ) );

        assertTrue( strHtml.contains( "<strong>closed<\\/strong>" ), strHtml );
        assertFalse( strHtml.contains( "onerror" ), strHtml );
    }

    /**
     * The attributes of the buttons, given as params maps, are written by htmlAttributes in strict mode.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testButtonAttributesFromParams( ) throws Exception
    {
        String strHtml = render( TEMPLATE_MANAGE_DESKS, model( ) );

        assertTrue( strHtml.contains( "data-bs-target=\"#commentModal\"" ), strHtml );
        assertTrue( strHtml.contains( "data-bs-target=\"#deskModal\"" ), strHtml );
        assertTrue( strHtml.contains( "data-bs-dismiss=\"modal\"" ), strHtml );
    }
}
