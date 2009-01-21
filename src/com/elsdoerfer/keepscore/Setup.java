package com.elsdoerfer.keepscore;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class Setup extends Activity {
	
	DbAdapter mDb = new DbAdapter(this);
	
	// views
	protected ListView mExistingPlayersList;
	protected EditText mNewPlayerNameText;
	protected Button mAddNewPlayerButton;	
	protected Button mStartNewGameButton;
	protected LinearLayout mExistingSessionsPanel;
	protected ListView mExistingSessionsList;
	
	// menu items
	public static final int CLEAR_PLAYERS_ID = Menu.FIRST;
	public static final int CONTINUE_GAME_ID = Menu.FIRST + 1;
	public static final int DELETE_GAME_ID = Menu.FIRST + 2;
	public static final int CLEAR_GAMES_ID = Menu.FIRST + 3;	
	protected MenuItem mClearPlayersItem;
	protected MenuItem mDeleteGameItem;
	protected MenuItem mClearGamesItem;	
		
	protected ArrayList<String> mListOfPlayersArray;
	protected ArrayAdapter<String> mListOfPlayersAdapter;
	protected SimpleCursorAdapter mExistingSessionsAdapter;
	
	public static final String LIST_OF_PLAYERS = "players";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.setup);          
        
        // open database
        mDb = new DbAdapter(this);
        mDb.open();
        
        // get views
        mExistingPlayersList = (ListView)findViewById(R.id.existing_players);
        mNewPlayerNameText = (EditText)findViewById(R.id.new_player_name);
        mAddNewPlayerButton = (Button)findViewById(R.id.add_new_player);
        mStartNewGameButton = (Button)findViewById(R.id.start_game);
        mExistingSessionsPanel = (LinearLayout)findViewById(R.id.existing_sessions);
        mExistingSessionsList = (ListView)findViewById(R.id.existing_sessions_list);
        
        // prepare the list of players for a new session
        mListOfPlayersArray = savedInstanceState != null 
        	? savedInstanceState.getStringArrayList(LIST_OF_PLAYERS) 
        	: new ArrayList<String>();
        mListOfPlayersAdapter = new ArrayAdapter<String>(
        		this, android.R.layout.simple_list_item_1, mListOfPlayersArray);
    	mExistingPlayersList.setAdapter(mListOfPlayersAdapter);    	
    	
    	// prepare the list of existing sessions
    	final Cursor existingSessionListCursor = mDb.fetchAllSessions();
    	startManagingCursor(existingSessionListCursor);
    	mExistingSessionsAdapter = 
    		new SimpleCursorAdapter(
    				this, android.R.layout.simple_list_item_1, 
    				existingSessionListCursor, 
    				new String[] { "label" },  new int[] { android.R.id.text1 });
    	mExistingSessionsList.setAdapter(mExistingSessionsAdapter);        
        
        // setup event handlers - we need to refer to the context in some of them 
    	final Context context = this;
    	
    	this.registerForContextMenu(mExistingSessionsList);
    	
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
				String playerName = mNewPlayerNameText.getText().toString().trim();
				if (playerName.length()==0)
					return;
				addPlayerToNewGame(playerName);
				// clear field for new player
				mNewPlayerNameText.setText("");
				mNewPlayerNameText.requestFocus();				
			}        	
        });    
                
        mExistingPlayersList.setOnItemClickListener(new OnItemClickListener() {
			@Override			
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				final String selectedPlayer = mListOfPlayersAdapter.getItem(position);
				new AlertDialog.Builder(context)
                	.setIcon(android.R.drawable.ic_dialog_alert)
                	.setTitle("Remove player \"" + selectedPlayer + "\"?")
                	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                		public void onClick(DialogInterface dialog, int whichButton) {
                			mListOfPlayersAdapter.remove(selectedPlayer);
                			updateUI();
                		}
                	})
                	.setNegativeButton("No", null)
                	.create().show();
			}			     	
        });
        
        mStartNewGameButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDb.createSession((String[]) mListOfPlayersArray.toArray(new String[0]));
				existingSessionListCursor.requery();
				
				Intent intent = new Intent(context, Game.class);
				startActivity(intent);
														
				mNewPlayerNameText.setText("");
				mListOfPlayersAdapter.clear();
				updateUI();				
			}        	
        });
        
        mExistingSessionsList.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				sessionListSelectionChanged();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				sessionListSelectionChanged();
			}        
        });        
        
        mExistingSessionsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				continueSession(0);		
			}        	
        });
        
        // initial update
        updateUI();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(LIST_OF_PLAYERS, mListOfPlayersArray);
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	mClearPlayersItem = menu.add(0, CLEAR_PLAYERS_ID, 0, R.string.clear_players);
    	mDeleteGameItem = menu.add(0, DELETE_GAME_ID, 0, R.string.delete_session);
    	mClearGamesItem = menu.add(0, CLEAR_GAMES_ID, 0, R.string.clear_sessions);
    	// setup initial visibilities
    	updateUI();
    	sessionListSelectionChanged();
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case DELETE_GAME_ID:
    		deleteSession(mExistingSessionsList.getSelectedItemId());    		
    		return true;
    	case CLEAR_GAMES_ID:
    		new AlertDialog.Builder(this)
        	.setIcon(android.R.drawable.ic_dialog_alert)
        	.setTitle("This will permanently remove all saved sessions. Are you sure?")
        	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			mDb.clearSessions();
            		mExistingSessionsAdapter.getCursor().requery();
            		updateUI();
        		}
        	})
        	.setNegativeButton("No", null)
        	.create().show();    		
    		return true;
    	case CLEAR_PLAYERS_ID:
    		mListOfPlayersAdapter.clear();
    		updateUI();
    		return true;
    	}
    	return false;
    }
    
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.add(R.string.continue_session).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				continueSession(info.id);				
				return true;
			}        		
    	});
        menu.add(R.string.delete_session).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				deleteSession(info.id);
				return true;
			}        		
    	});                
    }    
    
    protected void addPlayerToNewGame(String playerName) {    	
    	mListOfPlayersAdapter.add(playerName);
    	updateUI();
    }   
    
    protected void continueSession(long id) {
    	Intent intent = new Intent(this, Game.class);
		startActivity(intent);			
    }
    
    protected void deleteSession(long id) {
		mDb.deleteSession(id);
		mExistingSessionsAdapter.getCursor().requery();
		updateUI();    	
    }
    
    protected void sessionListSelectionChanged() {
		if (mDeleteGameItem!=null)			
			mDeleteGameItem.setEnabled(mExistingSessionsList.getSelectedItem() != null);
    }
    
    protected void updateUI() {    	
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
            
        	// Also hide the whole sessions panel if there aren't any sessions
        	if (mExistingSessionsAdapter.isEmpty())
        		mExistingSessionsPanel.setVisibility(View.GONE);
        	else
        		mExistingSessionsPanel.setVisibility(View.VISIBLE);
    	}    
    	
        // Show/hide/enable menu items depending on the features 
    	// and controls  currently visible. 
    	if (mDeleteGameItem != null) {   // Menu might not have been created yet
	        boolean editingPlayers = !mListOfPlayersAdapter.isEmpty();
	        boolean sessionsExist = !mExistingSessionsAdapter.isEmpty();
	        mDeleteGameItem.setVisible(!editingPlayers);
	        mClearGamesItem.setVisible(!editingPlayers);
	        mClearGamesItem.setEnabled(sessionsExist);
	        mClearPlayersItem.setVisible(editingPlayers);
    	}
    	
    	// allow to start a new game only if min. 2 players
    	if (mListOfPlayersAdapter.getCount() >= 2)
    		mStartNewGameButton.setVisibility(View.VISIBLE);
    	else
    		mStartNewGameButton.setVisibility(View.GONE);        
    }
}