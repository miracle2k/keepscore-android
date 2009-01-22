/*
        Keep Score: keep track of player scores during a card game. 
        Copyright (C) 2009 Michael Elsdörfer <http://elsdoerfer.name>
        
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.
        
        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.
        
        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.elsdoerfer.keepscore;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {	
	
	///////////////////////////////////////////////////////////////
	
	public static String SESSION_TABLE = "session";
	public static String SESSION_ID_KEY = "_id";
	public static String SESSION_LAST_PLAYED_AT_KEY = "last_played_at";	
	
	public static String PLAYER_TABLE = "player";
	public static String PLAYER_ID_KEY = "_id";
	public static String PLAYER_SESSION_KEY = "session_id";
	public static String PLAYER_NAME_KEY = "name";
	public static String PLAYER_INDEX_KEY = "idx";
	
	public static String SCORE_TABLE = "score";
	public static String SCORE_ID_KEY = "_id";
	public static String SCORE_SESSION_KEY = "session_id";
	public static String SCORE_PLAYER_INDEX_KEY = "player_index";
	public static String SCORE_VALUE_KEY = "value";
	public static String SCORE_CREATED_AT_KEY = "created_at";
	
	private static final String[] DATABASE_CREATE = {
        "CREATE TABLE session (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
        "                      last_played_at UNSIGNED INTEGER NOT NULL);",
        
        "CREATE TABLE player (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "                     session_id INTEGER NOT NULL," + 
        "                     name TEXT NOT NULL," + 
        "                     idx INTEGER NOT NULL);",
        
        "CREATE TABLE score (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "                    session_id INTEGER NOT NULL," + 
        "                    player_index INTEGER NOT NULL," +
        "                    value INTEGER NOT NULL," +
        "                    created_at UNSIGNED INTEGER NOT NULL);"};
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
	    private static final String DATABASE_NAME = "data";
	    private static final int DATABASE_VERSION = 1;		

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.beginTransaction();
        	try {
	        	for (String statement : DATABASE_CREATE)
	        		db.execSQL(statement);
	        	db.setTransactionSuccessful();
        	} 
        	finally {
        		db.endTransaction();
        	}
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    }	
	
	
	///////////////////////////////////////////////////////////////
	
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private Context mContext;
	

    public DbAdapter(Context ctx) {
        mContext = ctx;
    }	
    
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mContext);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }
    
    
    ///////////////////////////////////////////////////////////////
    
    
    public long createSession(String[] players) {
    	mDb.beginTransaction();
    	try {
	    	ContentValues values = new ContentValues();
	    	values.put(SESSION_LAST_PLAYED_AT_KEY, new Date().getTime());    	
	    	long session_id = mDb.insert(SESSION_TABLE, null, values);
	    	for (int i=0; i<players.length; i++) {
	    		values = new ContentValues();
	    		values.put(PLAYER_SESSION_KEY, session_id);
	    		values.put(PLAYER_NAME_KEY, players[i]);
	    		values.put(PLAYER_INDEX_KEY, i);
	    		mDb.insert(PLAYER_TABLE, null, values);
	    	}
	    	mDb.setTransactionSuccessful();
	    	return session_id;
    	}
    	finally {
    		mDb.endTransaction();
    	}    	
    }
    
    public Cursor fetchAllSessions() {
    	return mDb.rawQuery(    			
			 "SELECT _id, GROUP_CONCAT(name, ', ') AS label, last_played_at "+
			 "FROM (SELECT session._id AS _id, last_played_at, player.name AS name FROM session "+
		     "      LEFT OUTER JOIN player ON session._id = player.session_id "+
		     "      ORDER BY player.idx ASC) " +
		     "GROUP BY _id "+
		     "ORDER BY last_played_at DESC", null);      
    }
    
    public String[] fetchSessionPlayerNames(long sessionId) {
    	Cursor cursor = mDb.query(PLAYER_TABLE, new String[]{PLAYER_NAME_KEY}, 
    			PLAYER_SESSION_KEY + "= ?", new String[]{String.valueOf(sessionId)}, 
    			null, null, PLAYER_INDEX_KEY + " ASC");    	
    	String[] result = new String[cursor.getCount()];
    	cursor.moveToFirst();
    	do {
    		result[cursor.getPosition()] = cursor.getString(0);
    	} while (cursor.moveToNext());
        return result;
    }
    
    public boolean updateSessionTimestamp(long sessionId) {
    	ContentValues args = new ContentValues();
        args.put(SESSION_LAST_PLAYED_AT_KEY, new Date().getTime());
        return mDb.update(SESSION_TABLE, args, SESSION_ID_KEY + "=" + sessionId, null) > 0;	
    }
    
    public void addSessionScores(long sessionId, Integer[] scores) {
    	// We trust that "scores" has the right size for this 
    	// session, and that the corresponding player "index" 
    	// values in the session go from 0 to [length].
    	mDb.beginTransaction();
    	try {
    		for (int i=0; i<scores.length; i++) {
	    		ContentValues values = new ContentValues();	    		
	    		values.put(SCORE_SESSION_KEY, sessionId);
	    		values.put(SCORE_PLAYER_INDEX_KEY, i);	    			    		
	    		values.put(SCORE_VALUE_KEY, scores[i]);
	    		values.put(SCORE_CREATED_AT_KEY, new Date().getTime());
	    		mDb.insert(SCORE_TABLE, null, values);    			
    		}    		
    		mDb.setTransactionSuccessful();
    	}
    	finally {
    		mDb.endTransaction();
    	}
    }
    
    public Cursor fetchSessionScores(long sessionId) {
    	return mDb.query(SCORE_TABLE, new String[]{SCORE_VALUE_KEY}, 
    			SCORE_SESSION_KEY + "= ?", new String[]{String.valueOf(sessionId)}, 
    			null, null, SCORE_CREATED_AT_KEY + " ASC, " + SCORE_PLAYER_INDEX_KEY + " ASC");    		
    }
    
    public void deleteSession(long sessionId) {
    	mDb.beginTransaction();
    	try {
	    	mDb.delete("session", "_id = " + sessionId, null);
	    	mDb.delete("player", "session_id = " + sessionId, null);
	    	mDb.delete("score", "session_id = " + sessionId, null);
	    	mDb.setTransactionSuccessful();
    	}
    	finally {
    		mDb.endTransaction();
    	}
    }
    
    public void clearSessions() {
    	mDb.beginTransaction();
    	try {
	    	mDb.delete("score", null, null);
	    	mDb.delete("player", null, null);
	    	mDb.delete("session", null, null);
	    	mDb.setTransactionSuccessful();
    	}
    	finally {
    		mDb.endTransaction();    		
    	}
    }
}
