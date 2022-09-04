# [Yandex Mobile Ads SDK](https://yandex.ru/dev/mobile-ads/doc/intro/about.html) for Defold

*This is an open-source project. It is not affiliated with Yandex LLC.*

*This extension is independent and unofficial, and not associated with Yandex LLC in any way.*


ATTENTION! Currently only Android SDK is supported! No iOs support in current implementation!


The Yandex Mobile Ads extension lets you display banner, interstitial and rewarded ads.

# API Overview

## Functions

- [`yandexads.enable_debug()`](#function_enable_debug)
- [`yandexads.init()`](#function_init)
- [`yandexads.load()`](#function_load)
- [`yandexads.is_loaded()`](#function_show)
- [`yandexads.hide_banner()`](#function_hide_banner)


# Project Settings

Open `game.project` and a new entry to the `dependencies` property:

- `https://github.com/osov/defold-yandex-ads/archive/master.zip`


Then select `Project -> Fetch Libraries` to download the extension in your project.


You need Defold version 1.2.165+

# Functions

## <a name="function_enable_debug">`yandexads.enable_debug()`</a>

Enables additional output for debugging purposes.

---

## <a name="function_init">`yandexads.init(params)`</a>

Initializes the extension. This function has to be called first, before using any other methods of the extension.

### params <sub>required</sub>
Table. Contains parameters for the call &mdash; see the next section for details.

## Parameter Reference

The `params` table includes parameters for the call.

### listener <sub>optional</sub>
Function. The callback function which receives all [yandexads](#event_yandexads) events.

## Example

```lua
-- Banner id


local function listener(event)
	print('yandexads event type', event.type)
	print('yandexads event phase', event.phase)
	if event.phase == 'init' then -- yandexads has been initialized, now it's safe to load a banner.
		yandexads.load{
			type = 'banner',
			id = 'R-M-DEMO-320x50',
			w = 320,
			h = 50
		}
	end
end

-- Init yandexads.
yandexads.init{
	listener = listener
}
```

---

## <a name="function_load">`yandexads.load(params)`</a>

Loads a specified ad unit. It also allows you to specify additional targeting parameters.

### params <sub>required</sub>
Table. Contains parameters for the call &mdash; see the next section for details.

## Parameter Reference

The `params` table includes parameters for the call.

### type <sub>optional</sub>
String. Type of the ad unit: `'banner'`, `'interstitial'` (default) or `'rewarded'`. 

### id <sub>required</sub>
String. Ad unit id, e.g. `'R-M-DEMO-rewarded-client-side-rtb'`.

### w <sub>optional</sub>
int. Banner size (width) to load: (default = 320)

### h <sub>optional</sub>
int. Banner size (height) to load: (default = 50)


## Example

```lua
-- Load rewarded video ad.
yandexads.load{
	type = 'rewarded',
	id = 'R-M-DEMO-rewarded-client-side-rtb'
}

-- Load banner ad.
yandexads.load{
	type = 'banner',
	id = 'R-M-DEMO-320x50',
	w = 320,
	h = 50
}
```

---

## <a name="function_is_loaded">`yandexads.is_loaded(type)`</a>

Returns `true` if the specified ad type has been loaded.

### type <sub>required</sub>
String. Which adverstiment type to check: `'banner'`, `'interstitial'` or `'rewarded'`.

## Example
```lua
print('Is an interstitial ad loaded? ' .. (yandexads.is_loaded('interstitial') and 'Yes' or 'No'))
```

---

## <a name="function_show">`yandexads.show(type)`</a>

Displays a loaded ads. Use [yandexads.load()](#function_load) to load an ad before calling this method.

You can check if an ad has been loaded with [yandexads.is_loaded()](#function_is_loaded) method or you can listen to the [yandexads](#event_yandexads) event with a loaded phase:
```lua
-- Inside yandexads listener.
if event.type == 'interstitial' and event.phase == 'loaded' then
	yandexads.show('interstitial')
end
```

Banners don't need this method because they are displayed automatically when loaded.

### type <sub>required</sub>
String. Which adverstiment type to display: `'interstitial'` or `'rewarded'`.

## Example

```lua
yandexads.show('rewarded')
```

---

## <a name="function_hide_banner">`yandexads.hide_banner()`</a>

Removes a loaded banner from the screen.

---

# Events

## <a name="event_yandexads">`yandexads`</a>

Occurs when something has happened with ad units or when the extension has been initialized.

## Properties Overview


#### [event.phase](#event_yandexads_phase)

#### [event.type](#event_yandexads_type)

## Properties

### <a name="event_yandexads_phase">`event.phase`</a>

String. Phase of an ad unit lifetime.

Possible values depend on the ad type [event.type](#event_yandexads_type). 

### banner

* `'failed_to_load'` - banner ad request failed, `is_error` is `true`.
* `'loaded'` - banner ad is loaded.

### interstitial

* `'failed_to_load'` - interstitial ad request failed, `is_error` is `true`.
* `'loaded'` - interstitial ad is loaded.

### rewarded

* `'failed_to_load'` - video ad request failed, `is_error` is `true`.
* `'loaded'` - video ad is loaded.
* `'rewarded'` - video ad has triggered a reward.

---

### <a name="event_yandexads_type">`event.type`</a>

String. indicates the ad unit type: `'banner'`, `'interstitial'` or `'rewarded'`. Or extension initialization - `'init'`.

---


# Usage

Use [yandexads.init()](#function_init) to initialize it when your app starts. A good place for that would be the `init()` function of some root game object. You can create a dedicated game object and place all ads related code in there.

Once the init process if finished (you can listen for the `'init'` phase/type in the [yandexads](#event_yandexads) event or you can just wait), you can start loading ads. It's good to preload ads before you actually need to display it. Use [yandexads.load()](#function_load) to load and [yandexads.show()](#function_show) to show the ads.

Banners are displayed automatically when loaded, no need to call [yandexads.show()](#function_show) for them, however to remove a banner, you would need to use [yandexads.hide_banner()](#function_hide_banner).
