# Personal bookmarks

This little webapp allows you to keep track of your bookmarks/TODO lists/whatever else you like,
from any device you want (so long as you have Internet access). This is intended for personal use;
if you share access with someone else, they'll have full edit access to whatever data they can see.
The "private tags" feature can limit shared access but is not particularly intended for such things.

The app is made using ClojureScript and based on `reagent`/`re-frame` (ReactJS) libraries; the cloud
backend/storage/hosting is Google Firebase. Additionally, following libraries and utilities are used:

* [BlueprintJS](https://github.com/palantir/blueprint) (UI components)
* [jsSHA](https://github.com/Caligatio/jsSHA) (passphrases handling)
* [MarkedJS](https://github.com/markedjs/marked) (markdown rendering)
* [qrcode-generator](https://github.com/kazuhikoarase/qrcode-generator) (QR code rendering)
* [shadow-cljs](https://github.com/thheller/shadow-cljs) (webapp building)
* [wisp](https://github.com/Gozala/wisp) (CLI scripts)


## Usage

The app usage, as well as its main features, are described [in this gallery](https://imgur.com/a/C8adQRs).

For a TL;DR version (what the app looks like) you can check out [screenshots](#screenshots) at the bottom.


## Setup

#### Prerequisites

* `git` (to work with repository)
* `yarn` (for dependencies/scripting)
* CLI for devtools (optional, install via `yarn global add` or `npm -g`):
  - `shadow-cljs` (for building/REPL)
  - `firebase-tools` (for deployment to Firebase hosting)
* a Google (GMail) account (for Firebase backend); a free Firebase plan should be enough for regular app usage

#### Preparing project

1. [Clone] or download the sources to your computer.
2. Run `yarn` in the project root to install dependencies.
3. Set up your Firebase backend:
  - create a project [in Firebase WebUI](https://console.firebase.google.com)
  - go to project settings and register a webapp
    + take app config from the Config section and save it as `firebase.json` in the project root
      (make sure it's [valid JSON](https://en.wikipedia.org/wiki/JSON#Example));
      this step can be skipped if you're going to deploy to Firebase hosting
      (file will be created during `firebase init` run)
  - in the Authentication section, enable Email/Password sign-in and add an account to log in as
    (external sign-in options aren't used to avoid requirement of logging in to any other website)
  - in Database section, open Realtime Database and set up Rules:
    ```json
    {
      "rules": {
        ".read": "auth.uid !== null",
        ".write": "auth.uid !== null",
        "urls": {
          ".indexOn": ["parent"]
        }
      }
    }
    ```

[Clone]: https://help.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository


#### Build

* Run `yarn watch` to start dev server (on `localhost:3000`)
  - alternatively, run `yarn repl` to start up REPL
    (then `(shadow/watch :app)` to start server, and `(shadow/repl :app)` to connect)
* Run `yarn release` to produce release build

#### Install

After successful run of `yarn release`, the `public/` folder will contain the release version of webapp.

To deploy it to Firebase backend, you'll need to install `firebase-tools` (see prerequisites).
[After that](https://firebase.google.com/docs/hosting/quickstart):

* run `firebase login` to access your Google account (in WebUI)
* run `firebase init` to link Firebase project and set up the app (single-page = yes, overwrite index.html = no);
  `firebase.json` will be overwritten
* make sure you've run `yarn release` to produce a working build
* run `firebase deploy` to upload the build

Now you can access the app from any linked URL (default ones are `<project-id>.web.app` and `<project-id>.firebaseapp.com`).

#### Backups

If you don't want to lose all of your data by accident (which *shouldn't* happend but who knows?), you can set up database backups.
To do so, you'll need to open the [Firebase WebUI](https://console.firebase.google.com), open Service Accounts in project settings,
and download a private key token from Firebase SDK section (save it as `firebase-token.json`).
Take note that if you lose the token file, you'll have to generate a new one.

Now you can download a database backup at any time using `yarn backup` command. You can set up crontab (or any other scheduler)
to run it regularly. Traffic limit for free accounts is 10 Gb/month so daily syncs won't be a problem unless your data grows over 300 Mb.

Backups will be saved as `backups/<UTC-ISO-datetime>.json`. You can restore them in Firebase WebUI (use Import JSON menu command
in Realtime Database root). Yes, you can do backups by hand (using Export JSON); the CLI command is for regular/scheduled backups.

Additionally, you can produce batch updates using `yarn convert` (run without arguments to get usage help). Though that's likely won't
be needed unless you're changing the app sources. Still, here's an example of how to fix lost entries (if you somehow manage to
remove an entry with existing children):
```clojure
yarn convert -o out.json '(fn [x k db] (if (get (:urls db) (:parent x)) x (l/assoc x :parent "")))'
```


## Screenshots

Main screen
![main screen](https://i.imgur.com/ZsrZrJq.png)
Entry details
![entry details](https://i.imgur.com/3KJI5L2.png)
Edit dialog
![edit dialog](https://i.imgur.com/mUqy896.png)
Private tags dialog
![private tags dialog](https://i.imgur.com/QwsP2Pr.png)
