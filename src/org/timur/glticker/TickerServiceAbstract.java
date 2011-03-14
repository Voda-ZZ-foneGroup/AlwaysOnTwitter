/*
 * Copyright (C) 2011 Timur Mehrvarz Duesseldorf
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

package org.timur.glticker;

import java.util.List;

public abstract class TickerServiceAbstract extends android.app.Service {
  public abstract void onResume();
  public abstract void onPause();

  public abstract boolean isConnected();
  public abstract boolean isConnecting();
  public abstract String getErrMsg();

  public abstract List<EntryTopic> getMessageListLatest(int maxCount);
  public abstract List<EntryTopic> getMessageListLatestAfterMS(long startMS, int maxCount);

  public abstract class LocalBinder extends android.os.Binder {
    public abstract TickerServiceAbstract getService();
  };
}

