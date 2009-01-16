package com.elsdoerfer.keepscore;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class Setup extends Activity {
	
	public static final int CLEAR_PLAYERS_ID = Menu.FIRST;
	public static final int DELETE_GAME_ID = Menu.FIRST + 1;
	public static final int CLEAR_GAMES_ID = Menu.FIRST + 2;	
	
	protected ListView mExistingPlayersList;
	protected EditText mNewPlayerNameText;
	protected Button mAddNewPlayerButton;	
	protected Button mStartNewGameButton;
	protected LinearLayout mExistingSessionsPanel;
	protected ListView mExistingSessionsList;
	
	protected MenuItem mClearPlayersItem;
	protected MenuItem mDeleteGameItem;
	protected MenuItem mClearGamesItem;	
	
	
	protected List<CharSequence> mListOfPlayersArray;
	protected ArrayAdapter<CharSequence> mListOfPlayersAdapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);        
        
        // get views
        mExistingPlayersList = (ListView)findViewById(R.id.existing_players);
        mNewPlayerNameText = (EditText)findViewById(R.id.new_player_name);
        mAddNewPlayerButton = (Button)findViewById(R.id.add_new_player);
        mStartNewGameButton = (Button)findViewById(R.id.start_game);
        mExistingSessionsPanel = (LinearLayout)findViewById(R.id.existing_sessions);
        mExistingSessionsList = (ListView)findViewById(R.id.existing_sessions_list);
        
        // stores the list of players of the new game
        mListOfPlayersArray = new ArrayList<CharSequence>();
        mListOfPlayersAdapter = new ArrayAdapter<CharSequence>(
        		this, android.R.layout.simple_list_item_1, mListOfPlayersArray);
    	mExistingPlayersList.setAdapter(mListOfPlayersAdapter);        
        
        // setup events
        mNewPlayerNameText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					mAddNewPlayerButton.performClick();				
					return true;
				}				
				return false;
			}        	
        });
        mAddNewPlayerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CharSequence playerName = mNewPlayerNameText.getText().toString().trim();
				if (playerName.length()==0)
					return;
				addPlayerToNewGame(playerName);
				// clear field for new player
				mNewPlayerNameText.setText("");
				mNewPlayerNameText.requestFocus();				
			}        	
        });    
        
        final Context context = this;
        mExistingPlayersList.setOnItemClickListener(new OnItemClickListener() {
			@Override			
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				final CharSequence selectedPlayer = mListOfPlayersAdapter.getItem(position);
				new AlertDialog.Builder(context)
                	.setIcon(android.R.drawable.ic_dialog_alert)
                	.setTitle("Remove player \"" + selectedPlayer + "\"?")
                	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                		public void onClick(DialogInterface dialog, int whichButton) {
                			mListOfPlayersAdapter.remove(selectedPlayer);
                			playerListChanged();
                		}
                	})
                	.setNegativeButton("No", null)
                	.create().show();
			}			     	
        });
        
        /*mStartNewGameButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {			
			}        	
        });*/
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	mClearPlayersItem = menu.add(0, CLEAR_PLAYERS_ID, 0, R.string.clear_players);
    	mDeleteGameItem = menu.add(0, DELETE_GAME_ID, 0, R.string.delete_session);
    	mClearGamesItem = menu.add(0, CLEAR_GAMES_ID, 0, R.string.clear_sessions);
    	// setup initial visibilities
        playerListChanged();
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case DELETE_GAME_ID:
    		return true;
    	case CLEAR_GAMES_ID:
    		return true;
    	case CLEAR_PLAYERS_ID:
    		mListOfPlayersAdapter.clear();
    		playerListChanged();
    		return true;
    	}
    	return false;
    }
    
    protected void addPlayerToNewGame(CharSequence playerName) {    	
    	mListOfPlayersAdapter.add(playerName);
    	playerListChanged();
    }
    
    protected void playerListChanged() {    	
    	// Hide "existing session" list once the user starts to add
    	// players for a new game. This is mostly for layout reasons,
    	// because we apparently can't really have two lists in the 
    	// same screen unless both are fixed height (the first
    	// list would push elements below it out of the screen) (*).
    	// 
    	// So we basically hide the session list when the player 
    	// starts to use the player list.
    	//
    	// (*) We could possible work with a parent ScrollView and
    	// making both lists wrap_content, i.e. the whole screen 
    	// would scroll, through both lists and the controls in 
    	// between. This wouldn't make for very good user interface 
    	// though, since the user would be responsible to scrolling
    	// the "player name" TextEdit into view when he wants to use it. 
    	if (!mListOfPlayersAdapter.isEmpty()) {
    		LinearLayout.LayoutParams params;
            params = (LinearLayout.LayoutParams) mExistingPlayersList.getLayoutParams();
            params.weight = 1;
            mExistingPlayersList.setLayoutParams(params);                      

            mExistingSessionsPanel.setVisibility(View.GONE);            
    	} else {
    		LinearLayout.LayoutParams params;
            params = (LinearLayout.LayoutParams) mExistingPlayersList.getLayoutParams();
            params.weight = 0;
            mExistingPlayersList.setLayoutParams(params);                      

            mExistingSessionsPanel.setVisibility(View.VISIBLE);
    	}
    	
        // Show/Hide menu items depending on the features 
    	// and controls  currently visible. 
    	if (mDeleteGameItem != null) {   // Menu might not have been created yet
	        boolean editingPlayers = !mListOfPlayersAdapter.isEmpty();        
	        mDeleteGameItem.setVisible(!editingPlayers);
	        mClearGamesItem.setVisible(!editingPlayers);
	        mClearPlayersItem.setVisible(editingPlayers);
    	}
    	
    	// allow to start a new game only if min. 2 players
    	if (mListOfPlayersAdapter.getCount() >= 2)
    		mStartNewGameButton.setVisibility(View.VISIBLE);
    	else
    		mStartNewGameButton.setVisibility(View.GONE);        
    }
}