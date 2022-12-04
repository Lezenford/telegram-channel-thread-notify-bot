# Channel thread notify telegram bot (Slack's thread functional)

Telegram bot for imitate Slack thread notifications.
Support notification from Telegram channel comment threads for channel administrators

## Description

Bot tracking post owner and all administrators which sent a message to thread. Each new message from that thread
transferring to personal chat each tracking admin but not more the last 3 messages for each thread.

Supported:

* sending text message as notification
* sending image as notification
* edit sent messages if source changed

Unsupported:

* deleted notifications if source message has been deleted

## Technical details

Bot supported long-polling and webhook mode

Bot supported h2 and postgresql databases

## Connection bot to channel

* Add bot to channel with administrator permissions
* Add bot to discussion group with administrator permissions
* Send command ``/bind`` to channel
* After successfully bind that message will change to ``Bind successfully completed``

## Use

* Build with ``./gradlew :bootJar``
* Before starting you need to define environment variables:
  * ``telegram.type`` with value ``webhook`` or ``long-polling``
  * ``telegram.token``
  * for webhook type need define ``telegram.webhook.url`` for example ``https://example.com``
* Start application with java 17 or higher

## Recommended group settings

* Sign messages enabled. Otherwise, topic initiator doesn't receive notification until he writes a message in the thread
* Remain Anonymous disable for discussion chat owner

### List available variables

* ``telegram.token`` - token for bot. Required
* ``telegram.type`` - choose ``webhook`` or ``long-poling``. Required
* ``telegram.secret-token`` - secret token for webhook mode. Random UUID by default. See
  more: [Telegram bot api](https://core.telegram.org/bots/api#setwebhook)
* ``telegram.limit.request-per-second`` - max available count of per second request to telegram bot api. 20 by default.
  See more: [Telegram bot api](https://core.telegram.org/bots/faq#my-bot-is-hitting-limits-how-do-i-avoid-this)
* ``telegram.limit.thread-count`` - size thread pool for async request to bot api. 5 by default
* ``telegram.user-update.rate`` - how frequency need check (in millis) list of administrators in every channel. Need for
  removing singed out administrators from subscribe. 600000 by default (10 min)
* ``telegram.user-update.delay`` - delay before first check (in millis) list of administrators in every channel after
  application start. 5000 by default (5 sec)
* ``telegram.long-polling.rate`` - how frequency (in millis) bot will be request updates from server. 500 by default (
  0.5 sec). See more: [Wiki](https://en.wikipedia.org/wiki/Push_technology#Long_polling)
* ``telegram.long-polling.delay`` - delay (in millis) before first update request after application start. 1000 by
  default (1 sec)
* ``telegram.webhook.url`` - server url for webhook request. Required for ``webhook`` mode
* ``telegram.webhook.prefix`` - additional path for ``telegram.webhook.url``. ``telegram`` by default


