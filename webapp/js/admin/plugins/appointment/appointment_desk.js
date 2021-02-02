/********************************/
/*  Appointment Day Tabs        */
/********************************/
function setDayTabs(){
	var ctx=window.location.search.split('&');
	if( ctx.length==3 ){
		var tabCtx=ctx[2].split('=');
		var tabGroup='[data-menu=\'' + tabCtx[1] + '\']';
		$(tabGroup).toggle();
	}
}

/********************************/
/*  Appointment Desk Functions  */
/********************************/
/* Add RDV in day view 			*/
function setDayView( appointments ){
    for ( i in appointments ) {
        /* Draw RDV in day view 	*/
        setRDV( appointments[i] );
	}
	formatSurbook();
}

/* function SetSurbook : Check the number of el in surbook	*/
function formatSurbook( ){
	var st = $('.desk-' + nDesks + '.is-surbook');	
	st.each(function(){
		var nLength = $(this).children('span').length, nLeft=0;
		if( nLength > 0 ){
			var nW = ( 80 / nLength ) + '%';
			$(this).children('span').each(function(){
				$(this).css('width', nW  )
				if( nLeft > 0 ){ $(this).css('left', nLeft + 'px' )} ;
				nLeft += 30
			});
			$(this).append('<div class="badge">' + nLength + '</div>');
		}
	});
	// var st = $('.desk-' + nDesks + '.is-surbook > .badge');	
	// st.click( 	function(){
	// 	var nTop=0;
	// 	$(this).siblings().css('width', '80%' ).css('left', 0 ).css('z-index', 100 ).each(function(){
	// 		$(this).css('top', nTop + 'px' )
	// 		nTop += 35;
	// 	});
	// });

}

/* function checkMultiRdv : Check if an appointement has a closed slot	*/
function checkMultiRdv( app, desk ){
	var isOccupied=false;
	for ( k in app.slots) {
		var st = $('.desk-' + desk + '.slot-' + app.slots[k].id + '.start-' + app.slots[k].start);	
		if( st.hasClass('slot-close') || st.hasClass('is-multi') ){
			isOccupied = true;
		}
	}
	return isOccupied;
}

/* function checkMultiRdv : Check if an appointement has a closed slot	*/
function checkUniRdv( nbPlaces, start ){
	let isOccupied=false, nbOccupied=0;
	for ( u=1; u <= nDesks; u++ ){
		nbOccupied++;
		let ut = $( '.desk-' + u + '.start-' + start );	
		if( ut.hasClass('slot-close') || ut.hasClass('is-multi') ){
			nbOccupied--;
		}
	}
	if( nbPlaces < nbOccupied ){
		isOccupied = true;
	} else {
		isOccupied = false;
	}
	return isOccupied;
}

/* function setMultiRdv : Multi RDV for multi Slot Form */
function setMultiRdv( app, desk ){
	var n=0;
	for ( k in app.slots) {
		var st = $('.desk-' + desk + '.slot-' + app.slots[k].id + '.start-' + app.slots[k].start);	
		if( desk === nDesks ) {
			st = $('.desk-' + desk + '.start-' + app.slots[k].start + '.is-surbook');
		}
		st.addClass('is-multi multi-' + app.id );
		if ( n === 0 ){
			st.html( setInfo( app ) ).addClass('is-first');
		}
		n++
	}
	st.addClass('is-last');
}

/* function setUniRdv : Multi RDV for non multi Slot Form */
function setUniRdv( app, desk ){
	/* Set extra multi values */
	var info=setInfo( app ), multiDesk=desk;	
	if ( checkUniRdv( app.places, app.slots[j].start ) ){
		for ( n=1; n <= app.places; n++ ){
			//multiDesk = checkUniRdv( app.places, app.slots[j].start ) ? nDesks : multiDesk;
			let xt = $( '.desk-' + multiDesk + '.start-' + app.slots[j].start );
			if ( ( xt.html().trim().length === 0  ) && ( !xt.hasClass( 'is-multi' ) ) && ( !xt.hasClass( 'slot-close' ) ) ){
				if( n===1){
					$( xt ).html( info ).addClass('is-first-inline');
				}
				$( xt ).addClass( 'is-multi' );
			} else {
				if( multiDesk < nDesks ){
					n--
				}
			}
			multiDesk++;
			if( n === app.places ){ xt.addClass('is-last-inline') }
		}
	} else {
		let xt = $( '.desk-' + nDesks + '.start-' + app.slots[j].start );
		$( xt ).append( info );
	}
}

/* function setInfo : label RDV */
function setInfo( app ){
	var strPlace='', detailInfo='<a target="_blank" href="' + appUrlDetail + app.id + '" ><i class="fas fa-link"></i></a>';
	if( app.places > 1 ){
		strPlace= isMultiSlot ? '<br>( Nb de places ' + app.places + ' )' : '(&nbsp;Nb de places ' + app.places + '&nbsp;)' ;
	}
	return '<span class="multi-' + app.id + '" title="' + app.lastName + ' ' + app.firstName + strPlace + '">' + app.lastName + ' ' + app.firstName + ' ' + strPlace + detailInfo + '</span>';	
}

/* function setRDV : Draw appointement in the day grid */
function setRDV( app ){
	/* For slots in the appointment [i] */
	for ( j in app.slots) {
		/* Iterate on desks */
		for ( x = 1; x <= nDesks; x++ ) {
			/* If appointement has multi places to set */
			if( app.places > 1 ){
				/* If appointement is multi sloted */
				if( isMultiSlot ){
					/* Check if a multi slot appointement has a closed slot*/
					hasClosed=checkMultiRdv( app, x )
					/* If a slot is closed or is multi rdv then shift elements to next desk */
					if ( !hasClosed ){ 
						setMultiRdv( app, x );
						return true;
					} 
				} else {
					/* Set appointments for non multi slots form */
					setUniRdv( app, x );
					return true;
				}
			} else {
				/* If appointement is unique */
				var st = $( '.desk-' + x + '.start-' + app.slots[j].start );
			    var info =  setInfo( app );
				if( x === nDesks ) {
					st = $('.desk-' + nDesks + '.start-' + app.slots[j].start + '.is-surbook');
					$( st ).append( info );
					break;
				}
				if ( ( st.html().trim().length === 0  ) && ( !st.hasClass( 'is-multi' ) ) && ( !st.hasClass( 'slot-close' ) ) ){
					$( st ).append( info );
					break;
				}
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
		ignore: "a, p",
		toggle: true,
		saveState: 10,
		lasso: {
			border: "4px dotted rgba( 0, 248, 41, 1)",
			borderRadius: "0",
			backgroundColor: "rgba(0, 248, 41, 0.5)",
		}       
	});

	selectable.table();
	selectable.on( "end", function( e, selected, unselected ){
		var slotData='',slotMarker='', idx=1;
		var a=selectable.getSelectedNodes();
		var slotSize = a.length;
		if ( slotSize > 0 ){
			b=selectable.getSelectedNodes();
			var slotSize = b.length;
			b.forEach( function( el ){
				slotData = slotData + el.dataset.slot;
				slotMarker = slotMarker + el.dataset.marker;
				if( idx < slotSize ) slotData = slotData + ',';
				if( idx < slotSize ) slotMarker = slotMarker + ',';
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
								var itemStore = isOpen==0 ? 'slot-close' : 'slot-open';
								sessionStorage.setItem('changedSlots', itemStore + ',' + slotMarker );
								$('#manage_appointmentdesk').submit();
							},
							error: function( jqXhr, textStatus, errorThrown ){
								var itemStore = isOpen==0 ? 'slot-close' : 'slot-open';
								sessionStorage.setItem('changedSlots', itemStore + ',' + slotMarker );
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