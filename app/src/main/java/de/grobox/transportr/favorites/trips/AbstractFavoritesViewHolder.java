package de.grobox.transportr.favorites.trips;

import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import de.grobox.transportr.R;

abstract class AbstractFavoritesViewHolder extends RecyclerView.ViewHolder {

	private final View layout;
	final ImageView icon;
	final ImageButton overflow;

	AbstractFavoritesViewHolder(View v) {
		super(v);
		layout = v;
		icon = v.findViewById(R.id.logo);
		overflow = v.findViewById(R.id.overflowButton);
	}

	@CallSuper
	void onBind(final FavoriteTripItem item, final FavoriteTripListener listener) {
		layout.setOnClickListener(v -> listener.onFavoriteClicked(item));
	}

}
