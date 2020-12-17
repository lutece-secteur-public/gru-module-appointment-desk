/********************************/
/*  Appointment Desk Functions  */
/********************************/
/* Add RDV in day view */
function setDayView( appointments ){
    for ( i in appointments ) {
        var hasClosed = false;
        /* If an appointement has multi slot */
        if( appointments[i].places > 1 ){
            /* Check if a multi slot appointement has a closed slot*/
            hasClosed=checkMultiRdv( appointments[i] )
        }
        /* Draw RDV in day view */
        setRDV( appointments[i], hasClosed );
    }
}

/* function checkMultiRdv : Check if an appointement has a closed slot	*/
function checkMultiRdv( app ){
	var isClosed=false;
	for ( k in app.slots) {
		var st = $( '.slot-' + app.slots[k].id + '.start-' + app.slots[k].start ).not('.is-multi').first();
		$( st ).addClass('is-multi multi-' + app.id );
		if( st.hasClass('slot-close') ){
			isClosed = true;
		}
	}
	return isClosed;
}

function setMultiSlot( el, app, info, surbook ){
	/* Get position info                */
	var elPos = el.offset(), elTop=Math.floor( elPos.top + 5 ), elLeft=Math.ceil( elPos.left + 15 ), elHeight = 40 * app.places, elWidth=Math.ceil( el.width() -20 ),  elClass='';
	if( surbook ) { 
        elWidth=100;
        elLeft=elLeft - 10;
		var surbTop=$('.surbook.top-'+ elTop ), nLeft=0;
		if( surbTop.length > 0 ){
            $(surbTop).each(function() {
                nLeft +=  $(this).width();
            });
			elLeft = elLeft + nLeft;
		}
		elClass=' surbook top-' + elTop; 
	}
	/* Set HTML for multi-slot elements */
	var newEl = '<div class="multi-slot multi-slot-' + app.id + elClass + '" style="top:' + elTop + 'px; left:' + elLeft + 'px; height:' + elHeight + 'px; width:' + elWidth + 'px"></div>';
	/* Add to DOM                       */
	$( 'body' ).append( newEl );
	multi = '.multi-slot-' + app.id;
	/* Set info in multi-slot           */
	$( multi ).html( info );
}

/* function setRDV : Draw appointement in the day grid */
function setRDV( app, surbook ){
	for ( j in app.slots) {
		for ( x = 1; x <= nDesks; x++ ) {
			/* If a slot is closed then shift elements to surbook */
			if ( app.places > 1  && surbook ){ 
				x=nDesks;
			};
			/* Set table item selector  */
			var st = $( '.desk-' + x + '.start-' + app.slots[j].start ), strPlace='';
			if ( app.places > 1 ){ 
				strPlace='( Nb de places ' + app.places + ' )';
			};
			/* Iterate on desk */
			var info = '<span>' + app.lastName + ' ' + app.firstName + ' ' + strPlace + '</span>'
			if ( x < nDesks ){
				/* If slot is not set or closed */
				if ( ( st.html().trim().length === 0  ) && ( !st.hasClass( 'slot-close' ) ) ){
					/* Set info in slot */
					$( st ).html( info );
					/* Get first multi element to create div placement over */
					if( j==0 && app.places > 1 ){ 
						setMultiSlot( st, app, info, false );
					};
					/* If set return to other appointment */
					break;
				}
			} else {
				/* Set info in surbook slot */
				if( app.places > 1 ){ 
					if( $('.multi-slot-' + app.id + '.surbook').length == 0 ){
						setMultiSlot( st, app, info, true );
					}
				} else {
					$( st ).append( info );
				};
			}
		}
	}
}

function setSelection(){
if( $('.selectable').length > 0 ){
	const table = document.querySelector("table");
	const selectable = new Selectable({
		appendTo: table,
		filter: table.querySelectorAll(".selectable"),
		toggle: true,
		saveState: 10,
		lasso: {
			border: "4px dotted rgba( 0, 248, 41, 1)",
			borderRadius: "0",
			backgroundColor: "rgba(0, 248, 41, 0.5)",
		}       
	});

	selectable.table();

	selectable.on( "end", function(e, selected, unselected) {
		var slotData='', idx=1;
		var a=selectable.getSelectedNodes();
		var slotSize = a.length;
		if ( slotSize > 0 ){
			b=selectable.getSelectedNodes();
			var slotSize = b.length;
			b.forEach( function( el ){
				slotData = slotData + el.dataset.slot;
				if( idx < slotSize ) slotData = slotData + ','
				idx++; 
			});
			slotData = '[' +  slotData.replace(/\'/ig,'"') + ']';
			// Show Modal
			$('#slottoggle').modal('toggle');
			$('#slottoggle').on('shown.bs.modal', function (event) {
				var sData = JSON.parse( slotData );
				if( sData[0].isOpen === "true" ){
					$('#doCloseSlot').prop('checked', true);		
				} else {
					$('#doOpenSlot').prop('checked', true);		
				}
				var modal = $(this), btnModal = modal.find('#saveSlotState'), action='', jspUrl = 'jsp/admin/plugins/appointment/modules/desk/ManageAppointmentDesks.jsp?action=', filteredSlotData=null; 
				btnModal.click( function( e ){
					var isOpen = modal.find('.modal-body input:checked').val();
					var JData = JSON.parse( slotData );
					/* Ajax 		*/	
					if( isOpen==0 ){
						action = 'closeAppointmentDesk';
						filteredSlotData = JData.filter( function (slot) {
							return slot.isOpen === 'true';
						});
					} else {
						action = 'openAppointmentDesk';
						filteredSlotData = JData.filter( function (slot) {
							return slot.isOpen === 'false';
						});
					}
					var newData =  JSON.stringify( filteredSlotData );
					if( filteredSlotData.length > 0 ){
						$.ajax({
							url: jspUrl + action,
							dataType: 'json',
							type: 'POST',
							cache: false,
							data:{'data': newData},
							success: function( data, textStatus, jQxhr ){
								$('#manage_appointmentdesk').submit();
							},
							error: function( jqXhr, textStatus, errorThrown ){
								$('#manage_appointmentdesk').submit();
							},
							xhrFields: {
								withCredentials: true
							},
							crossDomain: true
						});
					} else {
						$('#slottoggle').modal('hide');
					}
				});
			});
			$('#slottoggle').on('hidden.bs.modal', function (event) {
				selectable.clear()
			});
		}
	});
}
}