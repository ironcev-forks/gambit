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

package ru.ming13.gambit.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ru.ming13.gambit.R;
import ru.ming13.gambit.bus.BusProvider;
import ru.ming13.gambit.bus.CardSavedEvent;
import ru.ming13.gambit.bus.OperationCancelledEvent;
import ru.ming13.gambit.bus.OperationSavedEvent;
import ru.ming13.gambit.model.Card;
import ru.ming13.gambit.model.Deck;
import ru.ming13.gambit.util.Fragments;
import ru.ming13.gambit.util.Intents;

public class CardEditingActivity extends ActionBarActivity
{
	@InjectView(R.id.toolbar)
	Toolbar toolbar;

	@InjectExtra(Intents.Extras.DECK)
	Deck deck;

	@InjectExtra(Intents.Extras.CARD)
	Card card;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_operation);

		setUpInjections();

		setUpToolbar();

		setUpFragment();
	}

	private void setUpInjections() {
		ButterKnife.inject(this);

		Dart.inject(this);
	}

	private void setUpToolbar() {
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_cancel);
	}

	private void setUpFragment() {
		Fragments.Operator.at(this).set(getCardEditingFragment(), R.id.container_fragment);
	}

	private Fragment getCardEditingFragment() {
		return Fragments.Builder.buildCardEditingFragment(deck, card);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_bar_operation, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case android.R.id.home:
				BusProvider.getBus().post(new OperationCancelledEvent());
				return true;

			case R.id.menu_save:
				BusProvider.getBus().post(new OperationSavedEvent());
				return true;

			default:
				return super.onOptionsItemSelected(menuItem);
		}
	}

	@Subscribe
	public void onOperationCancelled(OperationCancelledEvent event) {
		finish();
	}

	@Subscribe
	public void onCardSaved(CardSavedEvent event) {
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		BusProvider.getBus().register(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		BusProvider.getBus().unregister(this);
	}
}
