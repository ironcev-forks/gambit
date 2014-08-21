/*
 * Copyright 2012 Artur Dryomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ming13.gambit.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.squareup.otto.Subscribe;
import com.viewpagerindicator.UnderlinePageIndicator;

import ru.ming13.gambit.R;
import ru.ming13.gambit.adapter.CardsPagerAdapter;
import ru.ming13.gambit.bus.BusProvider;
import ru.ming13.gambit.bus.DeckCardsOrderLoadedEvent;
import ru.ming13.gambit.bus.DeviceShakenEvent;
import ru.ming13.gambit.model.Deck;
import ru.ming13.gambit.provider.GambitContract;
import ru.ming13.gambit.task.DeckCardsOrderLoadingTask;
import ru.ming13.gambit.task.DeckCardsOrderResettingTask;
import ru.ming13.gambit.task.DeckCardsOrderShufflingTask;
import ru.ming13.gambit.task.DeckEditingTask;
import ru.ming13.gambit.util.Animations;
import ru.ming13.gambit.util.Fragments;
import ru.ming13.gambit.util.Intents;
import ru.ming13.gambit.util.Loaders;
import ru.ming13.gambit.util.Seismometer;

public class CardsPagerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener
{
	public static CardsPagerFragment newInstance(Deck deck) {
		CardsPagerFragment fragment = new CardsPagerFragment();

		fragment.setArguments(buildArguments(deck));

		return fragment;
	}

	private static Bundle buildArguments(Deck deck) {
		Bundle arguments = new Bundle();

		arguments.putParcelable(Fragments.Arguments.DECK, deck);

		return arguments;
	}

	private static enum CardsOrder
	{
		DEFAULT, SHUFFLE, ORIGINAL
	}

	private CardsOrder currentCardsOrder = CardsOrder.DEFAULT;

	private Seismometer seismometer;

	@Override
	public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
		return layoutInflater.inflate(R.layout.fragment_cards_pager, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setUpSeismometer();
		setUpActionBar();
		setUpCards();
	}

	private void setUpSeismometer() {
		seismometer = new Seismometer(getActivity());
	}

	private void setUpActionBar() {
		setHasOptionsMenu(true);
	}

	private void setUpCards() {
		setUpCardsAdapter();
		setUpCardsIndicator();
		setUpCardsContent();
	}

	private void setUpCardsAdapter() {
		getCardsPager().setAdapter(new CardsPagerAdapter(getActivity()));
	}

	private ViewPager getCardsPager() {
		return (ViewPager) getView().findViewById(R.id.pager_cards);
	}

	private void setUpCardsIndicator() {
		UnderlinePageIndicator cardsIndicator = (UnderlinePageIndicator) getView().findViewById(R.id.indicator_cards);
		cardsIndicator.setViewPager(getCardsPager());
	}

	private void setUpCardsContent() {
		getLoaderManager().initLoader(Loaders.CARDS, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle loaderArguments) {
		String sort = String.format("%s, %s", GambitContract.Cards.ORDER_INDEX, GambitContract.Cards.FRONT_SIDE_TEXT);

		return new CursorLoader(getActivity(), getCardsUri(), null, null, null, sort);
	}

	private Uri getCardsUri() {
		return GambitContract.Cards.getCardsUri(getDeck().getId());
	}

	private Deck getDeck() {
		return getArguments().getParcelable(Fragments.Arguments.DECK);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cardsLoader, Cursor cardsCursor) {
		getCardsAdapter().swapCursor(cardsCursor);

		setUpCurrentActionBar();

		if (getCardsAdapter().isEmpty()) {
			showMessage();
			setUpCardsCreationListener();
		} else {
			hideMessage();
			setUpCurrentCard();
			setUpCurrentCardsOrder();
		}
	}

	private CardsPagerAdapter getCardsAdapter() {
		return (CardsPagerAdapter) getCardsPager().getAdapter();
	}

	private void setUpCurrentActionBar() {
		getActivity().invalidateOptionsMenu();
	}

	private void showMessage() {
		ViewAnimator animator = (ViewAnimator) getView().findViewById(R.id.animator);
		animator.setDisplayedChild(animator.indexOfChild(getView().findViewById(R.id.layout_message)));
	}

	private void setUpCardsCreationListener() {
		getView().findViewById(R.id.button_create_cards).setOnClickListener(this);
	}

	@Override
	public void onClick(View button) {
		startCardCreationStack();
	}

	private void startCardCreationStack() {
		getActivity().startActivities(new Intent[]{
			Intents.Builder.with(getActivity()).buildCardsListIntent(getDeck()),
			Intents.Builder.with(getActivity()).buildCardCreationIntent(getDeck())});
	}

	private void hideMessage() {
		ViewAnimator animator = (ViewAnimator) getView().findViewById(R.id.animator);

		if (animator.getCurrentView().getId() != R.id.layout_pager) {
			animator.setDisplayedChild(animator.indexOfChild(getView().findViewById(R.id.layout_pager)));
		}
	}

	private void setUpCurrentCard() {
		if (shouldSetCurrentCard()) {
			setUpCurrentCard(getDeck().getCurrentCardPosition());
		}
	}

	private boolean shouldSetCurrentCard() {
		return (currentCardsOrder == CardsOrder.DEFAULT) && (getCardsPager().getCurrentItem() == 0);
	}

	private void setUpCurrentCard(int currentCard) {
		getCardsPager().setCurrentItem(currentCard);
	}

	private void setUpCurrentCardsOrder() {
		if (shouldSetCurrentCardsOrder()) {
			DeckCardsOrderLoadingTask.execute(getActivity().getContentResolver(), getDeck());
		}
	}

	private boolean shouldSetCurrentCardsOrder() {
		return currentCardsOrder == CardsOrder.DEFAULT;
	}

	@Subscribe
	public void onCardsOrderLoaded(DeckCardsOrderLoadedEvent event) {
		switch (event.getCardsOrder()) {
			case SHUFFLE:
				currentCardsOrder = CardsOrder.SHUFFLE;
				break;

			case ORIGINAL:
				currentCardsOrder = CardsOrder.ORIGINAL;
				break;

			default:
				throw new RuntimeException();
		}

		setUpCurrentActionBar();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cardsLoader) {
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		super.onCreateOptionsMenu(menu, menuInflater);

		if (shouldActionsBeShown()) {
			menuInflater.inflate(R.menu.action_bar_cards_pager, menu);

			menu.findItem(R.id.menu_shuffle).setIcon(getShuffleActionIconResource());
			menu.findItem(R.id.menu_shuffle).setTitle(getShuffleActionTitleResource());
		}
	}

	private boolean shouldActionsBeShown() {
		return (getCardsAdapter() != null) && (getCardsAdapter().getCount() > 1);
	}

	private int getShuffleActionIconResource() {
		switch (currentCardsOrder) {
			case SHUFFLE:
				return R.drawable.ic_menu_shuffle_enabled;

			default:
				return R.drawable.ic_menu_shuffle_disabled;
		}
	}

	private int getShuffleActionTitleResource() {
		switch (currentCardsOrder) {
			case SHUFFLE:
				return R.string.menu_shuffle_disable;

			default:
				return R.string.menu_shuffle_enable;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case R.id.menu_replay:
				replayCards();
				return true;

			case R.id.menu_shuffle:
				switchCardsOrder();
				return true;

			case R.id.menu_flip:
				flipCards();
				return true;

			default:
				return super.onOptionsItemSelected(menuItem);
		}
	}

	private void replayCards() {
		getCardsPager().setCurrentItem(0);
	}

	private void switchCardsOrder() {
		switch (currentCardsOrder) {
			case ORIGINAL:
				shuffleCards();
				break;

			default:
				orderCards();
				break;
		}
	}

	private void shuffleCards() {
		DeckCardsOrderShufflingTask.execute(getActivity().getContentResolver(), getDeck());

		switchCardsOrder(CardsOrder.SHUFFLE);
	}

	private void switchCardsOrder(CardsOrder cardsOrder) {
		currentCardsOrder = cardsOrder;

		animateCardsShaking();

		setUpCurrentActionBar();
	}

	private void animateCardsShaking() {
		Animations.shake(getCardsPager());
	}

	private void orderCards() {
		DeckCardsOrderResettingTask.execute(getActivity().getContentResolver(), getDeck());

		switchCardsOrder(CardsOrder.ORIGINAL);
	}

	@Subscribe
	public void onDeviceShaken(DeviceShakenEvent event) {
		shuffleCards();
	}

	private void flipCards() {
		getCardsAdapter().switchDefaultCardSide();
		getCardsAdapter().notifyDataSetChanged();
	}

	@Override
	public void onResume() {
		super.onResume();

		seismometer.enable();

		BusProvider.getBus().register(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		seismometer.disable();

		BusProvider.getBus().unregister(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		saveCurrentCard();
	}

	private void saveCurrentCard() {
		Deck deck = new Deck(getDeck().getId(), getDeck().getTitle(), getCardsPager().getCurrentItem());

		getArguments().putParcelable(Fragments.Arguments.DECK, deck);

		DeckEditingTask.executeSilently(getActivity().getContentResolver(), deck);
	}
}