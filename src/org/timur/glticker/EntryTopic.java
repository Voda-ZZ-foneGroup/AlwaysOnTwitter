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

public class EntryTopic
{
  public int     regionID = 0;
  public String  channelName = null;
  public String  shortName = null;
  public int     priority = 0;
  public String  feedName = null;
  public String  feedTemplate = null;
  public String  title = null;
  public String  description = null;
  public String  link = null;
  public long    createTimeMs;
  public long    id = 0l;
  public String  imageUrl = null;

  public EntryTopic() {
  }
  
  public EntryTopic(int regionID, 
                    int priority, 
                    String channelName, 
                    String title, 
                    String description, 
                    String link, 
                    long createTimeMs) {
    this.regionID = regionID;
    this.priority = priority;
    this.channelName = channelName;
    this.title = title;
    this.description = description;
    this.link = link;
    this.createTimeMs = createTimeMs;
  }

  public EntryTopic(int regionID, 
                    int priority, 
                    String channelName, 
                    String title, 
                    String description, 
                    String link, 
                    long createTimeMs,
                    long id) {
    this(regionID, priority, channelName, title, description, link, createTimeMs);
    this.id = id;
  }

  public EntryTopic(int regionID, 
                    int priority, 
                    String channelName, 
                    String title, 
                    String description, 
                    String link, 
                    long createTimeMs,
                    long id,
                    String imageUrl) {
    this(regionID, priority, channelName, title, description, link, createTimeMs, id);
    this.imageUrl = imageUrl;
  }
}

