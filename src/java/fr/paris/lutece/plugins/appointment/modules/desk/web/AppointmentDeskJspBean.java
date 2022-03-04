/*
 * Copyright (c) 2002-2021, City of Paris
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

/*
 * Copyright (c) 2002-2020, Mairie de Paris
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

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.appointment.business.appointment.Appointment;
import fr.paris.lutece.plugins.appointment.business.form.Form;
import fr.paris.lutece.plugins.appointment.business.planning.WeekDefinition;
import fr.paris.lutece.plugins.appointment.business.slot.Slot;
import fr.paris.lutece.plugins.appointment.modules.desk.service.AppointmentDeskService;
import fr.paris.lutece.plugins.appointment.modules.desk.util.IncrementSlot;
import fr.paris.lutece.plugins.appointment.modules.desk.util.IncrementingType;
import fr.paris.lutece.plugins.appointment.service.AppointmentResourceIdService;
import fr.paris.lutece.plugins.appointment.service.AppointmentService;
import fr.paris.lutece.plugins.appointment.service.AppointmentUtilities;
import fr.paris.lutece.plugins.appointment.service.CommentService;
import fr.paris.lutece.plugins.appointment.service.FormService;
import fr.paris.lutece.plugins.appointment.service.SlotService;
import fr.paris.lutece.plugins.appointment.service.WeekDefinitionService;
import fr.paris.lutece.plugins.appointment.web.AppointmentFormJspBean;
import fr.paris.lutece.plugins.appointment.web.AppointmentJspBean;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFilterDTO;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.plugins.appointment.web.dto.CommentDTO;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.mailinglist.AdminMailingListService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.date.DateUtil;
import net.sf.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * This class provides the user interface to manage AppointmentDesk features ( manage, create, modify, remove )
 */
@Controller( controllerJsp = "ManageAppointmentDesks.jsp", controllerPath = "jsp/admin/plugins/appointment/modules/desk/", right = AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM )
public class AppointmentDeskJspBean extends AbstractManageAppointmentDeskJspBean
{
    /**
     * 
     */
    private static final long serialVersionUID = -2761497602249048780L;

    // Templates
    private static final String TEMPLATE_MANAGE_APPOINTMENTDESKS = "/admin/plugins/appointment/modules/desk/manage_appointmentdesks.html";

    // Parameters
    private static final String PARAMETER_ID_FORM = "id_form";
    private static final String PARAMETER_CONTEXT = "context";
    private static final String PARAMETER_NUMB_DESK = "numb_desk";
    private static final String PARAMETER_DATE_DAY = "day";
    private static final String PARAMETER_DATA = "data";

    private static final String PARAMETER_ENDING_DATE = "ending_date";
    private static final String PARAMETER_STARTING_DATE = "starting_date";
    private static final String PARAMETER_STARTING_TIME = "starting_time";
    private static final String PARAMETER_ENDING_TIME = "ending_time";
    private static final String PARAMETER_INCREMENTING_VALUE = "incrementing_value";
    private static final String PARAMETER_TYPE = "type";
    // Properties for page titles
    private static final String PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTDESKS = "appointment-desk.manage_appointmentdesks.pageTitle";

    // Markers
    private static final String MARK_LOCALE = "language";
    private static final String MARK_MACRO_LOCALE = "locale";
    private static final String MARK_DATE_DAY = "day";
    private static final String MARK_ID_FORM = "idForm";
    private static final String MARK_LIST_COMMENTS = "list_comments";
    private static final String MARK_LIST_SLOT = "list_slot";
    private static final String MARK_LIST_APPOINTMENT = "list_appointment";
    private static final String MARK_LIST_TYPE = "list_types";
    private static final String MARK_FORM = "appointmentForm";
    private static final String MARK_ACTIVATE_EDIT_MODE = "activateEditMode";
    private static final String MARK_MAILING_LIST = "mailing_list";
    private static final String MARK_CONTEXT = "context";
    private static final String MARK_APPOINTMENT_DESK_ENABLED = "isDeskInstalled";

    // Properties

    // Validations

    // Views
    private static final String VIEW_MANAGE_APPOINTMENTDESKS = "manageAppointmentDesks";

    // Actions
    private static final String ACTION_CLOSE_APPOINTMENTDESK = "closeAppointmentDesk";
    private static final String ACTION_OPEN_APPOINTMENTDESK = "openAppointmentDesk";
    private static final String ACTION_INCREMENT_MAX_CAPACITY = "incrementMaxCapacity";

    // Infos
    private static final String JSON_KEY_ERROR = "error";
    private static final String JSON_KEY_SUCCESS = "success";

    private static final String PROPERTY_MESSAGE_ERROR_PARSING_JSON = "module.appointment.desk.error.parsing.json";
    private static final String PROPERTY_MESSAGE_ERROR_ACCESS_DENIED = "module.appointment.desk.error.access.denied";
    // Session variable to store working values
    private int _nMaxCapacity;
    private String _strContext;
    private AppointmentJspBean manageAppointments = new AppointmentJspBean( );

    /**
     * Build the Manage View
     * 
     * @param request
     *            The HTTP request
     * @return The page
     */
    @View( value = VIEW_MANAGE_APPOINTMENTDESKS, defaultView = true )
    public String getManageAppointmentDesks( HttpServletRequest request )
    {
        Plugin moduleAppointmentDesk = getPlugin( );
        boolean bIsDeskActive = ( moduleAppointmentDesk != null ) && moduleAppointmentDesk.isInstalled( );
        if ( !bIsDeskActive )
        {
           try
           {
            return manageAppointments.getManageAppointments( request );
           }
           catch (AccessDeniedException e)
           {
            throw new AppException( e.getMessage( ), e );
        }
        }
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );
        int nIdForm = Integer.parseInt( strIdForm );
        String strDayDate = request.getParameter( PARAMETER_DATE_DAY );
        String strContext = request.getParameter( PARAMETER_CONTEXT );
        _strContext = ( StringUtils.isNotEmpty( strContext ) ) ? strContext : _strContext;
        boolean activateEditMode = true;
        LocalDate dateDay = null;
        User user = getUser( );
        if ( !RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm, AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM,
                user ) )
        {
            activateEditMode = false;
        }
        Form form = FormService.findFormLightByPrimaryKey( nIdForm );
        HashMap<LocalDate, WeekDefinition> mapWeekDefinition = WeekDefinitionService.findAllWeekDefinition( nIdForm );
        if ( StringUtils.isNotEmpty( strDayDate ) )
        {
            Date dateofDay = DateUtil.formatDate( strDayDate, getLocale( ) );
            dateDay = dateofDay.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( );
        }
        else
        {
            dateDay = LocalDate.now( );
            strDayDate = DateUtil.getDateString( Date.from( dateDay.atStartOfDay( ).atZone( ZoneId.systemDefault( ) ).toInstant( ) ), getLocale( ) );
        }
        int appointmentDesk = 0;
        List<Slot> listSlot = SlotService.buildListSlot( nIdForm, mapWeekDefinition, dateDay, dateDay );
        if ( !listSlot.isEmpty( ) )
        {

            Slot slot = listSlot.stream( ).max( Comparator.comparing( Slot::getMaxCapacity ) ).orElseThrow( NoSuchElementException::new );
            appointmentDesk = slot.getMaxCapacity( );

        }
        Map<String, Object> model = getModel( );
        java.sql.Date dateSqlDaey = java.sql.Date.valueOf( dateDay );
        List<CommentDTO> listComment = CommentService.buildCommentDTO( CommentService.findListCommentsInclusive( dateSqlDaey, dateSqlDaey, nIdForm ) );

        AppointmentFilterDTO filter = new AppointmentFilterDTO( );
        filter.setIdForm( nIdForm );
        filter.setStartingDateOfSearch( java.sql.Date.valueOf( dateDay ) );
        filter.setEndingDateOfSearch( java.sql.Date.valueOf( dateDay ) );
        filter.setStatus( 0 );
        List<Appointment> listAppt = AppointmentService.findListAppointmentsByFilter( filter );
        _nMaxCapacity = appointmentDesk;

        model.put( MARK_ACTIVATE_EDIT_MODE, activateEditMode );
        model.put( MARK_CONTEXT, _strContext );
        model.put( MARK_LIST_COMMENTS, listComment );
        model.put( PARAMETER_NUMB_DESK, appointmentDesk );
        model.put( MARK_LIST_SLOT, listSlot );
        model.put( MARK_LIST_APPOINTMENT, listAppt );
        model.put( MARK_LOCALE, getLocale( ) );
        model.put( MARK_DATE_DAY, strDayDate );
        model.put( MARK_ID_FORM, nIdForm );
        model.put( MARK_FORM, form );
        model.put( MARK_MACRO_LOCALE, getLocale( ) );
        model.put( MARK_LIST_TYPE, getListTypes( ) );
        model.put( MARK_MAILING_LIST, AdminMailingListService.getMailingLists( getUser( ) ) );
        model.put( AppointmentUtilities.MARK_PERMISSION_ADD_COMMENT, String.valueOf(
                RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm, AppointmentResourceIdService.PERMISSION_ADD_COMMENT_FORM, user ) ) );
        model.put( AppointmentUtilities.MARK_PERMISSION_MODERATE_COMMENT, String.valueOf( RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm,
                AppointmentResourceIdService.PERMISSION_MODERATE_COMMENT_FORM, user ) ) );
        model.put( AppointmentUtilities.MARK_PERMISSION_ACCESS_CODE, user.getAccessCode( ) );
        model.put( MARK_APPOINTMENT_DESK_ENABLED, bIsDeskActive );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTDESKS, TEMPLATE_MANAGE_APPOINTMENTDESKS, model );
    }

    /**
     * Process the data capture form of a new appointmentdesk
     *
     * @param request
     *            The Http Request
     * @return The Jsp URL of the process result
     * @throws AccessDeniedException
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Action( ACTION_CLOSE_APPOINTMENTDESK )
    public String docloseAppointmentDesk( HttpServletRequest request )
    {
        JSONObject json = new JSONObject( );
        String strJson = request.getParameter( PARAMETER_DATA );
        ObjectMapper mapper = new ObjectMapper( );
        mapper.registerModule( new JavaTimeModule( ) );
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        AppLogService.debug( "appointmentDesk - Received strJson : " + strJson );

        List<Slot> listSlots;
        try
        {
            listSlots = mapper.readValue( strJson, new TypeReference<List<Slot>>( )
            {
            } );

        }
        catch( JsonProcessingException e )
        {

            AppLogService.error( PROPERTY_MESSAGE_ERROR_PARSING_JSON + e.getMessage( ), e );
            json.element( JSON_KEY_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_PARSING_JSON, getLocale( ) ) );

            return json.toString( );
        }

        if ( !RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, Integer.toString( listSlots.get( 0 ).getIdForm( ) ),
                AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM, (User) getUser( ) ) )
        {
            AppLogService.error( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM,
                    new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM ) );
            json.element( JSON_KEY_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_ACCESS_DENIED, getLocale( ) ) );

            return json.toString( );
        }

        AppointmentDeskService.closeAppointmentDesk( listSlots );

        json.element( JSON_KEY_SUCCESS, JSON_KEY_SUCCESS );
        return json.toString( );

    }

    /**
     * Process the change form of a appointmentdesk
     *
     * @param request
     *            The Http request
     * @return The Jsp URL of the process result
     * @throws AccessDeniedException
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Action( ACTION_OPEN_APPOINTMENTDESK )
    public String doOpenAppointmentDesk( HttpServletRequest request )
    {
        JSONObject json = new JSONObject( );
        String strJson = request.getParameter( PARAMETER_DATA );
        AppLogService.debug( "appointmentDesk - Received strJson : " + strJson );
        ObjectMapper mapper = new ObjectMapper( );
        mapper.registerModule( new JavaTimeModule( ) );
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

        List<Slot> listSlots;
        try
        {

            listSlots = mapper.readValue( strJson, new TypeReference<List<Slot>>( )
            {
            } );

        }
        catch( JsonProcessingException e )
        {

            AppLogService.error( PROPERTY_MESSAGE_ERROR_PARSING_JSON + e.getMessage( ), e );
            json.element( JSON_KEY_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_PARSING_JSON, getLocale( ) ) );

            return json.toString( );
        }

        if ( !RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, Integer.toString( listSlots.get( 0 ).getIdForm( ) ),
                AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM, (User) getUser( ) ) )
        {
            AppLogService.error( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM,
                    new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM ) );
            json.element( JSON_KEY_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_ACCESS_DENIED, getLocale( ) ) );

            return json.toString( );

        }

        AppointmentDeskService.openAppointmentDesk( listSlots, _nMaxCapacity );

        json.element( JSON_KEY_SUCCESS, JSON_KEY_SUCCESS );
        return json.toString( );

    }

    @Action( ACTION_INCREMENT_MAX_CAPACITY )
    public String doIncrementMaxCapacity( HttpServletRequest request ) throws AccessDeniedException
    {
        IncrementSlot incrementSlot = new IncrementSlot( );
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );
        if ( !RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm, AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM,
                (User) getUser( ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM );
        }
        populate( request, incrementSlot );

        AppointmentDeskService.incrementMaxCapacity( incrementSlot );

        return getManageAppointmentDesks( request );
    }

    /**
     * List of all the available type
     * 
     * @return the list of the type
     */
    private ReferenceList getListTypes( )
    {
        ReferenceList refListType = new ReferenceList( );
        refListType.addItem( IncrementingType.FULLTIME.getValue( ), I18nService.getLocalizedString( IncrementingType.FULLTIME.getKey( ), getLocale( ) ) );
        refListType.addItem( IncrementingType.HALFTIMEMORNING.getValue( ),
                I18nService.getLocalizedString( IncrementingType.HALFTIMEMORNING.getKey( ), getLocale( ) ) );
        refListType.addItem( IncrementingType.HALFTIMEAFTERNOON.getValue( ),
                I18nService.getLocalizedString( IncrementingType.HALFTIMEAFTERNOON.getKey( ), getLocale( ) ) );
        refListType.addItem( IncrementingType.LACE.getValue( ), I18nService.getLocalizedString( IncrementingType.LACE.getKey( ), getLocale( ) ) );

        return refListType;
    }

    /**
     * Populate a bean using parameters in http request
     * 
     * @param request
     *            http request
     * @param bean
     *            bean to populate
     */
    private void populate( HttpServletRequest request, IncrementSlot incrementSlot )
    {

        incrementSlot.setIdForm( Integer.parseInt( request.getParameter( PARAMETER_ID_FORM ) ) );
        incrementSlot.setEndingTime( request.getParameter( PARAMETER_ENDING_TIME ) );
        incrementSlot.setStartingTime( request.getParameter( PARAMETER_STARTING_TIME ) );
        incrementSlot.setIncrementingValue( Integer.parseInt( request.getParameter( PARAMETER_INCREMENTING_VALUE ) ) );
        incrementSlot.setEndingDate( DateUtil.formatDate( request.getParameter( PARAMETER_ENDING_DATE ), getLocale( ) ).toInstant( )
                .atZone( ZoneId.systemDefault( ) ).toLocalDate( ) );

        incrementSlot.setStartingDate( DateUtil.formatDate( request.getParameter( PARAMETER_STARTING_DATE ), getLocale( ) ).toInstant( )
                .atZone( ZoneId.systemDefault( ) ).toLocalDate( ) );

        int type = Integer.parseInt( request.getParameter( PARAMETER_TYPE ) );
        incrementSlot.setType( IncrementingType.valueOf( type ) );
    }
}
