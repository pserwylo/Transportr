package de.grobox.transportr.trips.detail;

import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Trip.Leg;

interface LegClickListener {

	void onLegClick(Leg leg);

	void onLocationClick(Location location);

}
