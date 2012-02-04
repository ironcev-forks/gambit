package app.android.simpleflashcards.ui;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import app.android.simpleflashcards.R;
import app.android.simpleflashcards.SimpleFlashcardsApplication;
import app.android.simpleflashcards.models.Card;
import app.android.simpleflashcards.models.Deck;
import app.android.simpleflashcards.models.ModelsException;


public class CardEditingActivity extends Activity
{
	private final Context activityContext = this;

	private String frontSideText;
	private String backSideText;

	private int cardId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_editing);

		processActivityMessage();

		initializeBodyControls();

		new SetupExistingCardDataTask().execute();
	}

	private void processActivityMessage() {
		cardId = ActivityMessager.getMessageFromActivity(this);
	}

	private void initializeBodyControls() {
		Button confirmButton = (Button) findViewById(R.id.confirmButton);
		confirmButton.setOnClickListener(confirmListener);
	}

	private final OnClickListener confirmListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			readUserDataFromFields();

			String userDataErrorMessage = getUserDataErrorMessage();

			if (userDataErrorMessage.isEmpty()) {
				new CardUpdatingTask().execute();
			}
			else {
				UserAlerter.alert(activityContext, userDataErrorMessage);
			}
		}
	};

	private void readUserDataFromFields() {
		EditText frontSideEdit = (EditText) findViewById(R.id.frondSideEdit);
		EditText backSideEdit = (EditText) findViewById(R.id.backSideEdit);

		frontSideText = frontSideEdit.getText().toString().trim();
		backSideText = backSideEdit.getText().toString().trim();
	}

	private String getUserDataErrorMessage() {
		String errorMessage;

		errorMessage = getFrontSideTextErrorMessage();
		if (!errorMessage.isEmpty()) {
			return errorMessage;
		}

		errorMessage = getBackSideTextErrorMessage();
		if (!errorMessage.isEmpty()) {
			return errorMessage;
		}

		return errorMessage;
	}

	private String getFrontSideTextErrorMessage() {
		if (frontSideText.isEmpty()) {
			return getString(R.string.enterFrontText);
		}

		return new String();
	}

	private String getBackSideTextErrorMessage() {
		if (backSideText.isEmpty()) {
			return getString(R.string.enterBackText);
		}

		return new String();
	}

	private class SetupExistingCardDataTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialogShowHelper progressDialogHelper;

		private Card card;

		@Override
		protected void onPreExecute() {
			progressDialogHelper = new ProgressDialogShowHelper();
			progressDialogHelper.show(activityContext, getString(R.string.gettingDeckName));
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				Deck deck = SimpleFlashcardsApplication.getInstance().getDecks().getDeckByCardId(cardId);
				card = deck.getCardById(cardId);
			}
			catch (ModelsException e) {
				return getString(R.string.someError);
			}

			return new String();
		}

		@Override
		protected void onPostExecute(String errorMessage) {
			if (errorMessage.isEmpty()) {
				frontSideText = card.getFrontSideText();
				backSideText = card.getBackSideText();

				updateCardDataInFields();
			}
			else {
				UserAlerter.alert(activityContext, errorMessage);
			}

			progressDialogHelper.hide();
		}
	}

	private void updateCardDataInFields() {
		EditText frontSideTextEdit = (EditText) findViewById(R.id.frondSideEdit);
		EditText backSideTextEdit = (EditText) findViewById(R.id.backSideEdit);

		frontSideTextEdit.setText(frontSideText);
		backSideTextEdit.setText(backSideText);
	}

	private class CardUpdatingTask extends AsyncTask<Void, Void, String>
	{
		private ProgressDialogShowHelper progressDialogHelper;

		@Override
		protected void onPreExecute() {
			progressDialogHelper = new ProgressDialogShowHelper();
			progressDialogHelper.show(activityContext, getString(R.string.updatingCard));
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				Deck deck = SimpleFlashcardsApplication.getInstance().getDecks().getDeckByCardId(cardId);
				Card card = deck.getCardById(cardId);

				card.setFrontSideText(frontSideText);
				card.setBackSideText(backSideText);
			}
			catch (ModelsException e) {
				return getString(R.string.someError);
			}

			return new String();
		}

		@Override
		protected void onPostExecute(String errorMessage) {
			progressDialogHelper.hide();

			if (errorMessage.isEmpty()) {
				finish();
			}
			else {
				UserAlerter.alert(activityContext, errorMessage);
			}
		}
	}
}
