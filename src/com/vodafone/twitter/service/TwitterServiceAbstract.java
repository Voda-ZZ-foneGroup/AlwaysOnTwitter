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

package com.vodafone.twitter.service;

import org.timur.glticker.TickerServiceAbstract;
import twitter4j.*;

public abstract class TwitterServiceAbstract extends TickerServiceAbstract {
  public abstract Twitter getTwitterObject();
  public abstract String linkify(String msgString, String linkName, boolean twitterMode);
  public abstract String linkifyLink();
  public abstract void twitterLogin(String pin);
  public abstract void clearTwitterLogin();
}

