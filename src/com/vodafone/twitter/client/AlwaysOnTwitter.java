/*
 * Copyright (C) 2011 Vodafone Group Duesseldorf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vodafone.twitter.client;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import android.util.Log;
import android.util.Config;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.BroadcastReceiver;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.net.Uri;
import org.timur.glticker.EntryTopic;
import com.vodafone.twitter.service.TwitterService;
import com.vodafone.twitter.*;

public class AlwaysOnTwitter extends ListActivity {
  private final static String LOGTAG = "AlwaysOnTwitter";
  private final static String serviceClassName = "com.vodafone.twitter.service.TwitterService";
  private final static String NEW_MSG_BROADCAST_ACTION = "com.vodafone.AlwaysOnTwitterBroadcast"; 
//  private final static String CONSUMERKEY = "_____________________";
//  private final static String CONSUMERSECRET = "_________________________________________";
  private final static int messageListMaxSize = 40;
  private final static int maxRequestElements = 40;

  private LinkedList<EntryTopic> messageList = new LinkedList<EntryTopic>();
  private TwitterService mService = null;
  private volatile boolean activityPaused = false;
  private volatile int pullUpdateCounter = 0;
  private long newestMessageMS = 0l;
  private volatile boolean activityDestroying = false;
  private ServiceConnection serviceConnection = null;
  private ArrayAdapter<String> arrayAdapter = null;
  private long startServiceTime = 0l;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if(Config.LOGD) Log.i(LOGTAG, "onCreate");
    final Context context = this;
    activityDestroying = false;

    ArrayList<String> arrayList = new ArrayList<String>();
    arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, arrayList);
    setListAdapter(arrayAdapter);

    ListView listView = getListView();
    listView.setTextFilterEnabled(true);
    listView.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CharSequence charSequence = ((TextView)view).getText();
        if(charSequence!=null) {
          String messageString = charSequence.toString();
          if(Config.LOGD) Log.i(LOGTAG, "onItemClick() messageString="+messageString);
          String currentLink = null;
          if(mService!=null) {
            mService.linkify(messageString, null, true);
            currentLink = mService.linkifyLink();
          }
          if(Config.LOGD) Log.i(LOGTAG, "onItemClick() currentLink="+currentLink);
          if(currentLink!=null) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentLink));
            try {
              context.startActivity(browserIntent);
            } catch(android.content.ActivityNotFoundException acrnfex) {
              Log.e(LOGTAG, "openCurrentMsgInBrowser() ActivityNotFoundException for link="+currentLink);
              Toast.makeText(context, "no activity found for "+currentLink, Toast.LENGTH_SHORT).show(); 
            }
          }
        }
      }
    });
    
    Intent serviceIntent = new Intent();
    serviceIntent.setClassName(this, serviceClassName);
    serviceIntent.putExtra("consumerKey", Constants.CONSUMERKEY);
    serviceIntent.putExtra("consumerSecret", Constants.CONSUMERSECRET);
    serviceIntent.putExtra("intentFilter", NEW_MSG_BROADCAST_ACTION);
    startService(serviceIntent);

    bindService(serviceIntent, 
      serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
          // activity connected to service (not yet "service connected to twitter")
          if(Config.LOGD) Log.i(LOGTAG, "onServiceConnected() localBinder.getService() ...");
          TwitterService.LocalBinder localBinder = (TwitterService.LocalBinder)binder;
          mService = localBinder.getService();
          if(mService==null) {
            Log.e(LOGTAG, "onServiceConnected() no interface to service, mService==null");
            Toast.makeText(context, "Error - failed to get service interface from binder", Toast.LENGTH_LONG).show();
          } else {
            startServiceTime = System.currentTimeMillis();
          }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
          // as a result of unbindService()
          if(Config.LOGD) Log.i(LOGTAG, "onServiceDisconnected()");
          mService = null;
        }
      },
      Context.BIND_AUTO_CREATE);
    if(Config.LOGD) Log.i(LOGTAG, "onCreate bindService() done");

    // start listening to one specific type of broadcast (coming from the service)
    if(Config.LOGD) Log.i(LOGTAG, "onCreate registerReceiver() ...");
    registerReceiver(broadcastReceiver, new IntentFilter(NEW_MSG_BROADCAST_ACTION));

    // background thread to display service errors and fetch new tweets from service
    (new Thread() { public void run() {
      while(!activityDestroying) {
        if(mService!=null) {
          // toast all service error messages
          final String errMsg = mService.getErrMsg();
          if(errMsg!=null) {
            if(Config.LOGD) Log.i(LOGTAG, "onCreate background thread service errMsg="+errMsg);
            runOnUiThread(new Runnable() {
              public void run() { Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show();  }
            });
          }

          // copy all newly received tweets into arrayAdapter
          while(messageList.size()>0) {
            final EntryTopic entryTopic = messageList.removeFirst();
            if(Config.LOGD) Log.i(LOGTAG, "NEW MSG: "+entryTopic.channelName+": "+entryTopic.title);
            runOnUiThread(new Runnable() {
              public void run() { arrayAdapter.add(entryTopic.channelName+": "+entryTopic.title); }
            });
          }
        }
        try { Thread.sleep(200); } catch(Exception ex) { }
      }
    }}).start();

    if(Config.LOGD) Log.i(LOGTAG, "onCreate() done");
  }

  @Override 
  protected void onResume() {
    // activity will start interacting with the user (after onStart() or after onPause())
    // glView.onSurfaceCreated() will happen in parallel
    if(Config.LOGD) Log.i(LOGTAG, "onResume");
    super.onResume(); 
    activityPaused = false;

    if(mService!=null) {
      mService.onResume(); // -> if not connected, and no ConnectThread is pending, then start a new connectThread (which will fetch the timeline)

      if(Config.LOGD) Log.i(LOGTAG, "onResume isConnected="+mService.isConnected()+" currentMs="+System.currentTimeMillis()+" startServiceTime="+startServiceTime+" diff="+(System.currentTimeMillis()-startServiceTime));
      if(mService.isConnected() && System.currentTimeMillis()-startServiceTime>3000l) {
        int readCount = pullUpdateFromService();
        if(Config.LOGD) Log.i(LOGTAG, "onResume service isConnected; readCount="+readCount+" from pullUpdateFromServer()");
      }
    } else {
      if(Config.LOGD) Log.i(LOGTAG, "onResume no service handle");
    }
  }

  @Override 
  protected void onPause() {
    // activity will become invisible, other activity waits for this to finish before it gets into foreground (be quick)
    super.onPause();
    activityPaused = true;
    if(Config.LOGD) Log.i(LOGTAG, "onPause");
    if(mService!=null)
      mService.onPause();
  }

  @Override
  protected void onDestroy() {
    if(Config.LOGD) Log.i(LOGTAG, "onDestroy() ...");
    activityDestroying=true;

    if(serviceConnection!=null) {
      unbindService(serviceConnection);
      if(Config.LOGD) Log.i(LOGTAG, "destroy() unbindService() done");
      // our service will continue to run, since we used startService() in front of bindService()
      serviceConnection=null;
    }

    try {
      unregisterReceiver(broadcastReceiver);
    } catch(java.lang.IllegalArgumentException iaex) {
      Log.e(LOGTAG, "onDestroy() IllegalArgumentException "+iaex);
    }

    super.onDestroy();
    if(Config.LOGD) Log.i(LOGTAG, "onDestroy done");
  }

  public static final int MENU_HELP = 1;
  public static final int MENU_ABOUT = 9;

  @Override 
  public boolean onCreateOptionsMenu(Menu menu)
  {
    menu.add(Menu.NONE, MENU_HELP, Menu.NONE, "Help");
    menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, "About");
    return super.onCreateOptionsMenu(menu);
  }

  @Override 
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if(Config.LOGD) Log.i(LOGTAG, "onOptionsItemSelected()...");
    switch (item.getItemId())
    {
      case MENU_HELP:
        return true;
      case MENU_ABOUT:
        return true;
    }
    return false;
  }
  
  ////////////////////// private methods //////////////////////////
  
  private boolean findMsgIdInList(long id, LinkedList<EntryTopic> list) {
    int listSize = list.size();
    for(int i=0; i<listSize; i++) {
      try {
        if(list.get(i).id==id)
          return true;
      } catch(java.lang.IndexOutOfBoundsException ioobex) {
        Log.e(LOGTAG, "findMsgIdInList() i="+i+" listSize="+listSize+" IndexOutOfBoundsException "+ioobex);
      }
    }
    return false;
  }

  private void sendToMessageList(EntryTopic[] entryTopicArray) {
    if(entryTopicArray.length>0) {
      if(messageList!=null) {
        synchronized(messageList) {
	        for(int i=0; i<entryTopicArray.length; i++) {
            if(!findMsgIdInList(entryTopicArray[i].id,messageList)) {
              messageList.add(entryTopicArray[i]);
              if(messageList.size()>messageListMaxSize)
                messageList.removeFirst();
            }
          }
          //if(!activityPaused)
          //  messageList.notify();
        }
      }
    }
  }

  synchronized private int pullUpdateFromService() {
    int ret=0;
    pullUpdateCounter++;
    if(pullUpdateCounter<=1) {
      // get latest n messages from service
      if(mService==null) {
        Log.e(LOGTAG, "pullUpdateFromService abort on mService==null");
      } else {
        LinkedList<EntryTopic> tmpMessageList = new LinkedList<EntryTopic>();
        // retrieve the list of all messages (from the livecas service) which are newer than newestMessageMS
        List list = mService.getMessageListLatestAfterMS(newestMessageMS,maxRequestElements);
        if(list==null)
          Log.e(LOGTAG, "pullUpdateFromService abort on list==null");
        else
        synchronized(list) {
          int listSize = list.size();
          if(listSize<1) {
            //if(Config.LOGD) Log.i(LOGTAG, "pullUpdateFromService abort on list.size="+listSize);
          } else {
            int newItems=0;
            // add all new entries to the tail of the tmpMessageList, top of the newlist goes last, for keeping order
            for(int i=listSize-1; i>=0; i--) {
              // only add this entry, if it does not exist in tmpMessageList yet
              EntryTopic newEntryTopic = (EntryTopic)list.get(i);
              if(!findMsgIdInList(newEntryTopic.id,tmpMessageList)) {
                tmpMessageList.add(newEntryTopic);
                newItems++;
                //if(Config.LOGD) Log.v(LOGTAG, "pullUpdateFromService title="+newEntryTopic.title+" channel="+newEntryTopic.channelName); //+" descr="+newEntryTopic.description);

                // is this the timestamp of the newest message so far?
                if(newEntryTopic.createTimeMs>newestMessageMS)
                  newestMessageMS = newEntryTopic.createTimeMs;
              } else {
                if(Config.LOGD) Log.i(LOGTAG, "pullUpdateFromService duplicate entry="+newEntryTopic.title);    // possible to receive this
              }
            }

            // ok, so did we receive any NEW messages?
            int tmpMessageListSize = tmpMessageList.size();
            if(tmpMessageListSize<1) {
              //if(Config.LOGD) Log.i(LOGTAG, "pullUpdateFromService abort on tmpMessageListSize=<1");
            } else if(newItems<1) {
              //if(Config.LOGD) Log.i(LOGTAG, "pullUpdateFromService abort on newItems=<1");
            } else {
              // yes, we got some new messages
              while(tmpMessageListSize>messageListMaxSize) {
                tmpMessageList.removeFirst();
                tmpMessageListSize--;
              }
              sendToMessageList(tmpMessageList.toArray(new EntryTopic[0]));
              ret = tmpMessageListSize;
            }
          }
        }
      }
    }

    pullUpdateCounter--;
    return ret;
  }

  private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { 
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if(NEW_MSG_BROADCAST_ACTION.equals(action)) {
        //if(Config.LOGD) Log.i(LOGTAG, "BroadcastReceiver call pullUpdateFromService()...");
        pullUpdateFromService();
      }
    } 
  }; 
}

