/********************************/
/*  Appointment Desk Functions  */
/********************************/

/* Add RDV in day view */
function setDayView(appointments) {
	for (var i in appointments) {
		setRDV(appointments[i]);
	}
	formatSurbook();
}

/* Format surbook cells — distribute spans evenly */
function formatSurbook() {
	document.querySelectorAll('.desk-' + nDesks + '.is-surbook').forEach(function(cell) {
		var spans = cell.querySelectorAll('span');
		var nLength = spans.length, nLeft = 5;
		var nW = nLength > 1 ? (130 / nLength) - 0.15 + '%' : '80%';
		if (nLength > 0) {
			spans.forEach(function(span) {
				span.style.width = nW;
				span.style.left = nLeft + 'px';
				nLeft += 45;
			});
		}
	});
}

/* Check if a multi-slot appointment has a closed slot */
function checkMultiRdv(app, desk) {
	var isOccupied = false;
	for (var k in app.slots) {
		var st = document.querySelector('.desk-' + desk + '.slot-' + app.slots[k].id + '.start-' + app.slots[k].start);
		if (st && (st.classList.contains('slot-close') || st.classList.contains('is-multi'))) {
			isOccupied = true;
		}
	}
	return isOccupied;
}

/* Check if a uni-slot appointment has available desks */
function checkUniRdv(nbPlaces, start) {
	var nbOccupied = 0;
	for (var u = 1; u <= nDesks; u++) {
		nbOccupied++;
		var ut = document.querySelector('.desk-' + u + '.start-' + start);
		if (ut && (ut.classList.contains('slot-close') || ut.classList.contains('is-multi'))) {
			nbOccupied--;
		}
	}
	return nbPlaces >= nbOccupied;
}

/* Set multi-slot RDV */
function setMultiRdv(app, desk) {
	var n = 0, lastSt = null;
	for (var k in app.slots) {
		var st = document.querySelector('.desk-' + desk + '.slot-' + app.slots[k].id + '.start-' + app.slots[k].start);
		if (desk === nDesks) {
			st = document.querySelector('.desk-' + desk + '.start-' + app.slots[k].start + '.is-surbook');
		}
		if (!st) continue;
		st.classList.add('is-multi', 'multi-' + app.id);
		if (n === 0) {
			st.insertAdjacentHTML('beforeend', setInfo(app));
			st.classList.add('is-first');
		}
		n++;
		lastSt = st;
	}
	if (lastSt) {
		lastSt.classList.add('is-last');
	}
	var wSt = lastSt ? parseFloat(getComputedStyle(lastSt).width) : 0;
	var ft = document.querySelector('.desk-' + desk + '.is-multi.multi-' + app.id + '.is-first > .multi-' + app.id);
	if (ft) {
		ft.style.height = ((60 * n) - 15) + 'px';
		ft.style.width = (wSt - 8) + 'px';
	}
}

/* Set uni-slot RDV (multi places, non multi-slot form) */
function setUniRdv(app, desk) {
	var info = setInfo(app), multiDesk = desk, fst = null;
	if (checkUniRdv(app.places, app.slots[j].start)) {
		for (var n = 1; n <= app.places; n++) {
			var xt = document.querySelector('.desk-' + multiDesk + '.start-' + app.slots[j].start);
			if (xt && xt.innerHTML.trim().length === 0 && !xt.classList.contains('is-multi') && !xt.classList.contains('slot-close')) {
				if (n === 1) {
					xt.innerHTML = info;
					xt.classList.add('is-first-inline');
					fst = xt;
				}
				xt.classList.add('is-multi');
			} else {
				if (multiDesk < nDesks) { n--; }
			}
			multiDesk++;
			if (n === app.places) {
				xt.classList.add('is-last-inline');
				var wXt = parseInt(getComputedStyle(xt).width) + 3;
				var ft = document.querySelector('.is-multi.is-first-inline > .multi-' + app.id);
				if (ft) { ft.style.width = (wXt * n) + 'px'; }
			}
		}
	} else {
		var xt = document.querySelector('.desk-' + nDesks + '.start-' + app.slots[j].start);
		if (xt) { xt.insertAdjacentHTML('beforeend', info); }
	}
}

/* Build RDV label HTML */
function setInfo(app) {
	var strPlace = '';
	var detailInfo = '<a target="_blank" href="' + appUrlDetail + app.id + '"><i class="ti ti-link"></i></a>';
	if (app.places > 1) {
		strPlace = isMultiSlot ? '<br>( Nb de places ' + app.places + ' )' : '(&nbsp;Nb de places ' + app.places + '&nbsp;)';
	}
	return '<span class="multi-' + app.id + '" title="' + app.lastName + ' ' + app.firstName + strPlace + '">' + app.lastName + ' ' + app.firstName + ' ' + strPlace + detailInfo + '</span>';
}

/* Draw appointment in the day grid */
function setRDV(app) {
	for (j in app.slots) {
		for (var x = 1; x <= nDesks; x++) {
			if (app.places > 1) {
				if (isMultiSlot) {
					if (!checkMultiRdv(app, x)) {
						setMultiRdv(app, x);
						return true;
					}
				} else {
					setUniRdv(app, x);
					return true;
				}
			} else {
				var st = document.querySelector('.desk-' + x + '.start-' + app.slots[j].start);
				var info = setInfo(app);
				if (x === nDesks) {
					st = document.querySelector('.desk-' + nDesks + '.start-' + app.slots[j].start + '.is-surbook');
					if (st) { st.insertAdjacentHTML('beforeend', info); }
					break;
				}
				if (st && st.innerHTML.trim().length === 0 && !st.classList.contains('is-multi') && !st.classList.contains('slot-close')) {
					st.insertAdjacentHTML('beforeend', info);
					break;
				}
			}
		}
	}
}

/* Table selection for slot open/close */
function setSelection() {
	if (document.querySelectorAll('.selectable').length === 0) return;

	var table = document.querySelector('table');
	var selectable = new Selectable({
		appendTo: table,
		filter: table.querySelectorAll('.selectable'),
		ignore: 'a, p',
		toggle: true,
		saveState: 10,
		lasso: {
			border: '4px dotted rgba(0, 248, 41, 1)',
			borderRadius: '0',
			backgroundColor: 'rgba(0, 248, 41, 0.5)'
		}
	});

	selectable.table();
	selectable.on('end', function(e, selected, unselected) {
		var slotData = '', slotMarker = '', idx = 1;
		var nodes = selectable.getSelectedNodes();
		var slotSize = nodes.length;
		if (slotSize === 0) return;

		nodes.forEach(function(el) {
			slotData += el.dataset.slot;
			slotMarker += el.dataset.marker;
			if (idx < slotSize) { slotData += ','; slotMarker += ','; }
			idx++;
		});
		slotData = '[' + slotData.replace(/'/g, '"') + ']';

		var modalEl = document.getElementById('slottoggle');
		var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
		modal.show();

		modalEl.addEventListener('shown.bs.modal', function onShown() {
			// modalEl.removeEventListener('shown.bs.modal', onShown);
			var sData = JSON.parse(slotData);
			if (sData[0].isOpen === 'true') {
				document.getElementById('doCloseSlot').checked = true;
			} else {
				document.getElementById('doOpenSlot').checked = true;
			}

			document.getElementById('saveSlotState').addEventListener('click', function onClick() {
				// document.getElementById('saveSlotState').removeEventListener('click', onClick);
				var isOpen = document.querySelector('#slottoggle .modal-body input:checked').value;
				var JData = JSON.parse(slotData);
				var action = '', filteredSlotData = null;
				var jspUrl = 'jsp/admin/plugins/appointment/modules/desk/ManageAppointmentDesks.jsp?action=';

				if (isOpen == 0) {
					action = 'closeAppointmentDesk';
					filteredSlotData = JData.filter(function(slot) { return slot.isOpen === 'true'; });
				} else {
					action = 'openAppointmentDesk';
					filteredSlotData = JData.filter(function(slot) { return slot.isOpen === 'false'; });
				}

				if (filteredSlotData.length > 0) {
					var newData = JSON.stringify(filteredSlotData);
					fetch(jspUrl + action, {
						method: 'POST',
						headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
						body: 'data=' + encodeURIComponent(newData),
						credentials: 'same-origin'
					}).finally(function() {
						var itemStore = isOpen == 0 ? 'slot-close' : 'slot-open';
						sessionStorage.setItem('changedSlots', itemStore + ',' + slotMarker);
						document.getElementById('manage_appointmentdesk').submit();
					});
				} else {
					modal.hide();
				}
			});
		});

		modalEl.addEventListener('hidden.bs.modal', function onHidden() {
			// modalEl.removeEventListener('hidden.bs.modal', onHidden);
			selectable.clear();
		});
	});
}

/* Set comment background indicators on hour rows */
function setCommentsBg(event, currentDate, locale) {
	var bgColor = 'rgb(233, 250, 0)';
	if (event.comment_end_time !== '00:00' && event.comment_start_time !== '00:00') {
		var cellHeadFirst = document.querySelector('[data-head-start="' + event.comment_start_time + '"]');
		var cellHeadLast = document.querySelector('[data-head-start="' + event.comment_end_time + '"]');

		if (cellHeadFirst) {
			cellHeadFirst.insertAdjacentHTML('beforeend', '<i class="ti ti-message comment-' + event.id_comment + '" style="color:' + bgColor + '"></i>');
			cellHeadFirst.classList.add('hour-comment', 'hour-comment-first', 'comment-' + event.id_comment);
		}
		if (cellHeadLast) {
			cellHeadLast.insertAdjacentHTML('beforeend', '<i class="ti ti-message comment-' + event.id_comment + '" style="color:' + bgColor + '"></i>');
			cellHeadLast.classList.add('hour-comment', 'hour-comment-last', 'comment-' + event.id_comment);
		}

		if (cellHeadFirst) {
			var row = cellHeadFirst.closest('tr');
			while (row && (row = row.nextElementSibling)) {
				var firstChild = row.querySelector('td:first-child');
				if (!firstChild) break;
				if (firstChild.classList.contains('hour-comment-last') && firstChild.classList.contains('comment-' + event.id_comment)) {
					break;
				}
				firstChild.insertAdjacentHTML('beforeend', '<i class="ti ti-message comment-' + event.id_comment + '" style="margin-left:4px;color:' + bgColor + '"></i>');
				firstChild.classList.add('hour-comment', 'hour-comment-first', 'comment-' + event.id_comment);
			}
		}
	}
}

/* Highlight slot changes after save */
function slotChangeHighlight() {
	var slotsChanged = sessionStorage.getItem('changedSlots');
	var aSlotsChanged = slotsChanged ? slotsChanged.split(',') : [];
	var slotClass = '';
	if (aSlotsChanged.length > 0) {
		aSlotsChanged.forEach(function(item, index) {
			if (index > 0) {
				var el = document.querySelector(item);
				if (el) {
					if (el.classList.contains(slotClass)) {
						if (el.classList.contains('is-multi')) { slotClass += '-multi'; }
						el.classList.add('border-change-' + slotClass);
					} else {
						var row = el.closest('tr');
						if (row) {
							row.querySelectorAll('td.' + slotClass).forEach(function(td) {
								td.classList.add('border-change-' + slotClass);
							});
						}
					}
				}
			} else {
				slotClass = item;
			}
		});
	}
	sessionStorage.removeItem('changedSlots');
}
