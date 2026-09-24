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

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFilterDTO;
import fr.paris.lutece.plugins.appointment.web.dto.AppointmentFormDTO;
import fr.paris.lutece.plugins.appointment.web.dto.CommentDTO;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.mailinglist.AdminMailingListService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.date.DateUtil;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.NoSuchElementException;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * This class provides the user interface to manage AppointmentDesk features ( manage, create, modify, remove )
 */
@SessionScoped
@Named
@Controller( controllerJsp = "ManageAppointmentDesks.jsp", controllerPath = "jsp/admin/plugins/appointment/modules/desk/", right = AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM )
public class AppointmentDeskJspBean extends AbstractManageAppointmentDeskJspBean
{
    /**
     *
     */
    private static final long serialVersionUID = -2761497602249048780L;

    @Inject
    private Models _models;

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
    private static final String PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTDESKS = "module.appointment.desk.manage_appointmentdesks.pageTitle";

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
    private static final String PROPERTY_MESSAGE_ERROR_INCREMENT_INVALID = "module.appointment.desk.error.increment.invalid";
    private static final ObjectMapper MAPPER = new ObjectMapper( ).registerModule( new JavaTimeModule( ) )
            .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
    // Session variable to store working values
    private int _nMaxCapacity;
    private String _strContext;

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
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );
        int nIdForm = NumberUtils.toInt( strIdForm, -1 );
        Form form = FormService.findFormLightByPrimaryKey( nIdForm );
        if ( form == null )
        {
            return redirect( request, AppointmentFormJspBean.getURLManageAppointmentForms( request ) );
        }
        String strDayDate = request.getParameter( PARAMETER_DATE_DAY );
        String strContext = request.getParameter( PARAMETER_CONTEXT );
        _strContext = StringUtils.isNotEmpty( strContext ) ? strContext : StringUtils.defaultString( _strContext );
        boolean activateEditMode = true;
        LocalDate dateDay = null;
        User user = getUser( );
        if ( !RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm, AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM,
                user ) )
        {
            activateEditMode = false;
        }
        HashMap<LocalDate, WeekDefinition> mapWeekDefinition = WeekDefinitionService.findAllWeekDefinition( nIdForm );
        if ( StringUtils.isNotEmpty( strDayDate ) )
        {
            Date dateofDay = DateUtil.formatDate( strDayDate, getLocale( ) );
            if ( dateofDay != null )
            {
                dateDay = dateofDay.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( );
            }
            else
            {
                try
                {
                    dateDay = LocalDate.parse( strDayDate );
                }
                catch( Exception e )
                {
                    dateDay = LocalDate.now( );
                }
                strDayDate = DateUtil.getDateString( Date.from( dateDay.atStartOfDay( ).atZone( ZoneId.systemDefault( ) ).toInstant( ) ), getLocale( ) );
            }
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
        java.sql.Date dateSqlDaey = java.sql.Date.valueOf( dateDay );
        List<CommentDTO> listComment = CommentService.buildCommentDTO( CommentService.findListCommentsInclusive( dateSqlDaey, dateSqlDaey, nIdForm ) );

        AppointmentFilterDTO filter = new AppointmentFilterDTO( );
        filter.setIdForm( nIdForm );
        filter.setStartingDateOfSearch( java.sql.Date.valueOf( dateDay ) );
        filter.setEndingDateOfSearch( java.sql.Date.valueOf( dateDay ) );
        filter.setStatus( 0 );
        List<Appointment> listAppt = AppointmentService.findListAppointmentsByFilter( filter );
        _nMaxCapacity = appointmentDesk;

        _models.put( MARK_ACTIVATE_EDIT_MODE, activateEditMode );
        _models.put( MARK_CONTEXT, _strContext );
        _models.put( MARK_LIST_COMMENTS, listComment );
        _models.put( PARAMETER_NUMB_DESK, appointmentDesk );
        _models.put( MARK_LIST_SLOT, listSlot );
        _models.put( MARK_LIST_APPOINTMENT, listAppt );
        _models.put( MARK_LOCALE, getLocale( ) );
        _models.put( MARK_DATE_DAY, strDayDate );
        _models.put( MARK_ID_FORM, nIdForm );
        _models.put( MARK_FORM, form );
        _models.put( MARK_MACRO_LOCALE, getLocale( ) );
        _models.put( MARK_LIST_TYPE, getListTypes( ) );
        _models.put( MARK_MAILING_LIST, AdminMailingListService.getMailingLists( getUser( ) ) );
        _models.put( AppointmentUtilities.MARK_PERMISSION_ADD_COMMENT, String.valueOf(
                RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm, AppointmentResourceIdService.PERMISSION_ADD_COMMENT_FORM, user ) ) );
        _models.put( AppointmentUtilities.MARK_PERMISSION_MODERATE_COMMENT, String.valueOf( RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, strIdForm,
                AppointmentResourceIdService.PERMISSION_MODERATE_COMMENT_FORM, user ) ) );
        _models.put( AppointmentUtilities.MARK_PERMISSION_ACCESS_CODE, user.getAccessCode( ) );
        _models.put( MARK_APPOINTMENT_DESK_ENABLED, ( moduleAppointmentDesk != null ) && moduleAppointmentDesk.isInstalled( ) );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTDESKS, TEMPLATE_MANAGE_APPOINTMENTDESKS );
    }

    /**
     * Close one desk on the slots the desk screen selected.
     *
     * @param request
     *            The Http request, whose data parameter carries the slots as JSON
     * @return The JSON answer: success, or the error to show
     */
    @Action( ACTION_CLOSE_APPOINTMENTDESK )
    public String docloseAppointmentDesk( HttpServletRequest request )
    {
        return processSlots( request, AppointmentDeskService::closeAppointmentDesk );
    }

    /**
     * Open one desk on the slots the desk screen selected.
     *
     * @param request
     *            The Http request, whose data parameter carries the slots as JSON
     * @return The JSON answer: success, or the error to show
     */
    @Action( ACTION_OPEN_APPOINTMENTDESK )
    public String doOpenAppointmentDesk( HttpServletRequest request )
    {
        return processSlots( request, listSlots -> AppointmentDeskService.openAppointmentDesk( listSlots, _nMaxCapacity ) );
    }

    /**
     * Read the slots sent by the desk screen and apply an operation to them, once the user is proven to hold the right
     * on the form of every slot: the form of a stored slot is the stored one, never the one the screen sent.
     *
     * @param request
     *            The Http request, whose data parameter carries the slots as JSON
     * @param operation
     *            The operation on the slots
     * @return The JSON answer: success, or the error to show
     */
    private String processSlots( HttpServletRequest request, Consumer<List<Slot>> operation )
    {
        ObjectNode json = MAPPER.createObjectNode( );
        List<Slot> listSlots;
        try
        {
            listSlots = MAPPER.readValue( unescape( request.getParameter( PARAMETER_DATA ) ), new TypeReference<List<Slot>>( )
            {
            } );
        }
        catch( JsonProcessingException | IllegalArgumentException e )
        {
            AppLogService.error( "Error parsing the slots sent by the desk screen", e );
            listSlots = null;
        }
        if ( listSlots == null || listSlots.isEmpty( ) )
        {
            json.put( JSON_KEY_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_PARSING_JSON, getLocale( ) ) );
            return json.toString( );
        }
        for ( Slot slot : listSlots )
        {
            Slot stored = ( slot.getIdSlot( ) != 0 ) ? SlotService.findSlotById( slot.getIdSlot( ) ) : null;
            int nIdForm = ( stored != null ) ? stored.getIdForm( ) : slot.getIdForm( );
            if ( ( slot.getIdSlot( ) != 0 && stored == null ) || !RBACService.isAuthorized( AppointmentFormDTO.RESOURCE_TYPE, Integer.toString( nIdForm ),
                    AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM, (User) getUser( ) ) )
            {
                json.put( JSON_KEY_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_ACCESS_DENIED, getLocale( ) ) );
                return json.toString( );
            }
            slot.setIdForm( nIdForm );
        }
        operation.accept( listSlots );
        json.put( JSON_KEY_SUCCESS, JSON_KEY_SUCCESS );
        return json.toString( );
    }

    /**
     * Undo the HTML escaping the request filter applies to a JSON parameter.
     *
     * @param strJson
     *            The parameter
     * @return The JSON, or null
     */
    private static String unescape( String strJson )
    {
        if ( strJson == null )
        {
            return null;
        }
        String strPrevious;
        String strCurrent = strJson;
        do
        {
            strPrevious = strCurrent;
            strCurrent = StringEscapeUtils.unescapeHtml4( strCurrent );
        }
        while ( !strCurrent.equals( strPrevious ) );
        return strCurrent;
    }

    /**
     * Add places to the slots of a form between two dates, then show the desk screen again.
     *
     * @param request
     *            The Http request
     * @return The HTML of the desk screen
     * @throws AccessDeniedException
     *             If the user may not modify the form
     */
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
        if ( populate( request, incrementSlot ) )
        {
            AppointmentDeskService.incrementMaxCapacity( incrementSlot );
        }
        else
        {
            addError( PROPERTY_MESSAGE_ERROR_INCREMENT_INVALID, getLocale( ) );
        }

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
     * Fill the increment from the request.
     *
     * @param request
     *            The Http request
     * @param incrementSlot
     *            The increment to fill
     * @return true when every value of the request is valid: numbers, dates of the locale, a known type
     */
    private boolean populate( HttpServletRequest request, IncrementSlot incrementSlot )
    {
        Date startingDate = DateUtil.formatDate( request.getParameter( PARAMETER_STARTING_DATE ), getLocale( ) );
        Date endingDate = DateUtil.formatDate( request.getParameter( PARAMETER_ENDING_DATE ), getLocale( ) );
        int nIdForm = NumberUtils.toInt( request.getParameter( PARAMETER_ID_FORM ), -1 );
        int nValue = NumberUtils.toInt( request.getParameter( PARAMETER_INCREMENTING_VALUE ), 0 );
        IncrementingType type = IncrementingType.valueOf( NumberUtils.toInt( request.getParameter( PARAMETER_TYPE ), -1 ) );

        if ( startingDate == null || endingDate == null || nIdForm < 0 || nValue == 0 || type == null )
        {
            return false;
        }
        incrementSlot.setIdForm( nIdForm );
        incrementSlot.setEndingTime( request.getParameter( PARAMETER_ENDING_TIME ) );
        incrementSlot.setStartingTime( request.getParameter( PARAMETER_STARTING_TIME ) );
        incrementSlot.setIncrementingValue( nValue );
        incrementSlot.setStartingDate( startingDate.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( ) );
        incrementSlot.setEndingDate( endingDate.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDate( ) );
        incrementSlot.setType( type );
        return true;
    }
}
