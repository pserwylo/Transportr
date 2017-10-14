package de.grobox.liberario.trips.search;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import de.grobox.liberario.data.locations.LocationRepository;
import de.grobox.liberario.data.searches.SearchesRepository;
import de.grobox.liberario.locations.LocationsViewModel;
import de.grobox.liberario.networks.TransportNetwork;
import de.grobox.liberario.networks.TransportNetworkManager;
import de.grobox.liberario.trips.TripQuery;
import de.grobox.liberario.utils.SingleLiveEvent;
import de.schildbach.pte.NetworkProvider;
import de.schildbach.pte.NetworkProvider.WalkSpeed;
import de.schildbach.pte.dto.Product;
import de.schildbach.pte.dto.QueryTripsContext;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Trip;

import static de.schildbach.pte.dto.QueryTripsResult.Status.OK;

@ParametersAreNonnullByDefault
public class DirectionsViewModel extends LocationsViewModel {

	private final static String TAG = DirectionsViewModel.class.getSimpleName();

	private final SearchesRepository searchesRepository;

	private final MutableLiveData<Set<Trip>> trips = new MutableLiveData<>();
	private final SingleLiveEvent<String> queryError = new SingleLiveEvent<>();
	private final SingleLiveEvent<String> queryMoreError = new SingleLiveEvent<>();
	private @Nullable volatile QueryTripsContext queryTripsContext;

	@Inject
	DirectionsViewModel(TransportNetworkManager transportNetworkManager, LocationRepository locationRepository, SearchesRepository searchesRepository) {
		super(transportNetworkManager, locationRepository);
		this.searchesRepository = searchesRepository;
	}

	LiveData<Set<Trip>> getTrips() {
		return trips;
	}

	LiveData<String> getQueryError() {
		return queryError;
	}

	LiveData<String> getQueryMoreError() {
		return queryMoreError;
	}

	@SuppressLint("StaticFieldLeak")
	void search(TripQuery tripQuery) {
		Log.e(TAG, "QUERY FOR NEW TRIPS NOW~!!!!");

		// store/count search
		searchesRepository.storeSearch(tripQuery.toFavoriteTripItem());

		// reset current data
		clearState();

		new AsyncTask<TripQuery, Void, QueryTripsResult>() {
			@Override
			protected QueryTripsResult doInBackground(TripQuery... tripQueries) {
				try {
					return query(tripQueries[0]);
				} catch (Exception e) {
					queryError.postValue(e.toString());
					return null;
				}
			}
			@Override
			protected void onPostExecute(QueryTripsResult queryTripsResult) {
				if (queryTripsResult == null) return;
				if (queryTripsResult.status == OK && queryTripsResult.trips.size() > 0) {
					onQueryTripsResultReceived(queryTripsResult);
				} else {
					queryError.setValue(queryTripsResult.status.name());
				}
			}
		}.execute(tripQuery);
	}

	@WorkerThread
	private QueryTripsResult query(TripQuery query) throws IOException {
		TransportNetwork network = getTransportNetwork().getValue();
		if (network == null) throw new IllegalStateException("No transport network set");

		// TODO expose via TransportNetworkManager
		NetworkProvider.Optimize optimize = null; // TransportrUtils.getOptimize(getContext());
		WalkSpeed walkSpeed = null; // TransportrUtils.getWalkSpeed(getContext());
		EnumSet<Product> products = EnumSet.allOf(Product.class);

		Log.i(TAG, "From: " + query.from.getLocation());
		Log.i(TAG, "Via: " + (query.via == null ? "null" : query.via.getLocation()));
		Log.i(TAG, "To: " + query.to.getLocation());
		Log.i(TAG, "Date: " + query.date);
		Log.i(TAG, "Departure: " + query.departure);
//		Log.i(TAG, "Products: " + products);
//		Log.i(TAG, "Optimize for: " + optimize);
//		Log.i(TAG, "Walk Speed: " + walkSpeed);

		NetworkProvider np = network.getNetworkProvider();
		return np.queryTrips(query.from.getLocation(), query.via == null ? null : query.via.getLocation(), query.to.getLocation(),
				query.date, query.departure, products, optimize, walkSpeed, null, null);
	}

	@SuppressLint("StaticFieldLeak")
	void searchMore(boolean later) {
		new AsyncTask<Boolean, Void, QueryTripsResult>() {
			@Override
			protected QueryTripsResult doInBackground(Boolean... later) {
				try {
					return queryMore(later[0]);
				} catch (Exception e) {
					queryMoreError.postValue(e.toString());
					return null;
				}
			}
			@Override
			protected void onPostExecute(QueryTripsResult queryTripsResult) {
				if (queryTripsResult == null) return;
				if (queryTripsResult.status == OK && queryTripsResult.trips.size() > 0) {
					onQueryTripsResultReceived(queryTripsResult);
				} else {
					queryMoreError.setValue(queryTripsResult.status.name());
				}
			}
		}.execute(later);
	}

	@WorkerThread
	private QueryTripsResult queryMore(boolean later) throws IOException {
		TransportNetwork network = getTransportNetwork().getValue();
		if (network == null) throw new IllegalStateException("No transport network set");

		if (queryTripsContext == null) throw new IllegalStateException("No query context");
		if (later && !queryTripsContext.canQueryLater()) throw new IllegalStateException("Can not query later");
		if (!later && !queryTripsContext.canQueryEarlier()) throw new IllegalStateException("Can not query earlier");

		Log.i(TAG, "QueryTripsContext: " + queryTripsContext.toString());
		Log.i(TAG, "Later: " + later);

		NetworkProvider np = network.getNetworkProvider();
		return np.queryMoreTrips(queryTripsContext, later);
	}

	private void onQueryTripsResultReceived(QueryTripsResult queryTripsResult) {
		queryTripsContext = queryTripsResult.context;
		Set<Trip> oldTrips = trips.getValue();
		if (oldTrips == null) oldTrips = new HashSet<>();
		oldTrips.addAll(queryTripsResult.trips);
		trips.setValue(oldTrips);
	}

	private void clearState() {
		trips.setValue(null);
		queryTripsContext = null;
	}

}
