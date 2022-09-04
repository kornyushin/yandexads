package extension.yandexads;

import java.util.GregorianCalendar;
import java.util.Hashtable;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

import extension.yandexads.Utils.Scheme;
import extension.yandexads.Utils.Table;

@SuppressWarnings("unused")
public class Extension {
	private Activity activity;
	private boolean is_initialized = false;
	private BannerAdView mBannerAdView;
	private InterstitialAd mInterstitialAd;
	private RewardedAd mRewardedAd;
	private RelativeLayout mBannerLayout;
	private boolean interstitial_ad_is_loaded = false;
	private boolean rewarded_video_ad_is_loaded = false;
	private boolean banner_is_loaded = false;
	private LuaScriptListener script_listener = new LuaScriptListener();

	@SuppressWarnings("unused")
	public Extension(android.app.Activity main_activity) {
		activity = main_activity;
		Utils.set_tag("yandex-ads");
	}

	// Called from extension_android.cpp each frame.
	@SuppressWarnings("unused")
	public void update(long L) {
		Utils.execute_tasks(L);
	}

	@SuppressWarnings("unused")
	public void app_activate(long L) {
	}

	@SuppressWarnings("unused")
	public void app_deactivate(long L) {
	}

	@SuppressWarnings("unused")
	public void extension_finalize(long L) {
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean check_is_initialized() {
		if (is_initialized) {
			return true;
		} else {
			Utils.log("The extension is not initialized.");
			return false;
		}
	}

	// region Lua functions

	// yandexads.enable_debug()
	private int enable_debug(long L) {
		Utils.check_arg_count(L, 0);
		Utils.enable_debug();
		return 0;
	}

	// yandexads.init(params)
	private int init(long L) {
		Utils.debug_log("init()");
		Utils.check_arg_count(L, 1);
		Scheme scheme = new Scheme()
				.function("listener");

		Table params = new Table(L, 1).parse(scheme);

		Utils.delete_ref_if_not_nil(L, script_listener.listener);
		Utils.delete_ref_if_not_nil(L, script_listener.script_instance);
		script_listener.listener = params.get_function("listener", Lua.REFNIL);
		Lua.dmscript_getinstance(L);
		script_listener.script_instance = Utils.new_ref(L);

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				is_initialized = true;
				MobileAds.initialize(activity, new InitializationListener() {
					@Override
					public void onInitializationCompleted() {
						Utils.debug_log("onInitializationCompleted()");
						dispatch_event("init", "ads");
					}
				});
			}
		});

		return 0;
	}

	// yandexads.hide_banner()
	private int hide_banner(long L) {
		Utils.debug_log("hide_banner()");
		Utils.check_arg_count(L, 0);
		if (!check_is_initialized()) {
			return 0;
		}
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				destroyBannerUiThread();
			}
		});
		return 0;
	}

	// yandexads.load (params)
	private int load(long L) {
		Utils.debug_log("load()");
		Utils.check_arg_count(L, 1);
		if (!check_is_initialized()) {
			return 0;
		}
		Scheme scheme = new Scheme()
				.string("type")
				.string("id")
				.number("w")
				.number("h");
		Table params = new Table(L, 1).parse(scheme);
		final String type = params.get_string("type", "interstitial");
		final String id = params.get_string_not_null("id");
		final int w = params.get_integer("w", 320);
		final int h = params.get_integer("h", 50);
		if (type.equals("interstitial")) {
			interstitial_ad_is_loaded = false;
		} else if (type.equals("rewarded")) {
			rewarded_video_ad_is_loaded = false;
		} else if (type.equals("banner")) {
			banner_is_loaded = false;
		} else {
			Utils.debug_log("not correct type:" + type);
			Utils.debug_log("id:" + id);
			dispatch_event("notads", type);
		}

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (type.equals("interstitial")) {
					mInterstitialAd = new InterstitialAd(activity);
					mInterstitialAd.setAdUnitId(id);
					AdRequest adRequest = new AdRequest.Builder().build();

					mInterstitialAd.setInterstitialAdEventListener(new InterstitialAdEventListener() {
						@Override
						public void onAdLoaded() {
							interstitial_ad_is_loaded = true;
							Utils.debug_log("interstitial:onAdLoaded");
							dispatch_event("loaded", "interstitial");
						}

						@Override
						public void onAdFailedToLoad(AdRequestError adRequestError) {
							Utils.debug_log("interstitial:onAdFailedToLoad" + adRequestError.toString());
							dispatch_event("failedload" , "interstitial:"+adRequestError.toString());
						}

						@Override
						public void onAdShown() {
							Utils.debug_log("interstitial:onAdShown");
							dispatch_event("shown", "interstitial");
						}

						@Override
						public void onAdDismissed() {
							Utils.debug_log("interstitial:onAdDismissed");
							dispatch_event("dismissed", "interstitial");
						}

						@Override
						public void onAdClicked() {
							Utils.debug_log("interstitial:onAdClicked");
							dispatch_event("clicked", "interstitial");
						}

						@Override
						public void onLeftApplication() {
						}

						@Override
						public void onReturnedToApplication() {
						}

						@Override
						public void onImpression(@Nullable ImpressionData impressionData) {
							Utils.debug_log("interstitial:onImpression");
						}
					});
					mInterstitialAd.loadAd(adRequest);

				} else if (type.equals("rewarded")) {
					mRewardedAd = new RewardedAd(activity);
					mRewardedAd.setAdUnitId(id);
					AdRequest adRequest = new AdRequest.Builder().build();

					mRewardedAd.setRewardedAdEventListener(new RewardedAdEventListener() {
						@Override
						public void onRewarded(final Reward reward) {
							Utils.debug_log("rewarded:onRewarded");
							dispatch_event("rewarded", "rewarded");
						}

						@Override
						public void onAdClicked() {
							Utils.debug_log("rewarded:onAdClicked");
							dispatch_event("clicked", "rewarded");
						}

						@Override
						public void onAdLoaded() {
							rewarded_video_ad_is_loaded = true;
							Utils.debug_log("rewarded:onAdLoaded");
							dispatch_event("loaded", "rewarded");
						}

						@Override
						public void onAdFailedToLoad(final AdRequestError adRequestError) {
							Utils.debug_log("rewarded:onAdFailedToLoad");
							dispatch_event("failedload" , "rewarded:"+ adRequestError.toString());
						}

						@Override
						public void onAdShown() {
							Utils.debug_log("rewarded:onAdShown");
							dispatch_event("shown", "rewarded");
						}

						@Override
						public void onAdDismissed() {
							Utils.debug_log("rewarded:onAdDismissed");
							dispatch_event("dismissed", "rewarded");
						}

						@Override
						public void onLeftApplication() {
						}

						@Override
						public void onReturnedToApplication() {
						}

						@Override
						public void onImpression(@Nullable ImpressionData impressionData) {
							Utils.debug_log("rewarded:onImpression");
						}
					});

					// Загрузка объявления.
					mRewardedAd.loadAd(adRequest);

				} else if (type.equals("banner")) {
					destroyBannerUiThread();
					mBannerAdView = new BannerAdView(activity);
					mBannerAdView.setAdUnitId(id);
					mBannerAdView.setAdSize(AdSize.flexibleSize(w, h));
					final BannerAdView view = mBannerAdView;
					AdRequest adRequest = new AdRequest.Builder().build();
					mBannerAdView.setBannerAdEventListener(new BannerAdEventListener() {
						@Override
						public void onAdLoaded() {
							Utils.debug_log("banner:onAdLoaded");
							if (view != mBannerAdView) {
								Utils.debug_log("Prevent reporting onAdLoaded for obsolete BannerAd (loadBanner was called multiple times)");
								view.destroy();
								return;
							}
							banner_is_loaded = true;
							showBannerUiThread();
							dispatch_event("loaded", "banner");
						}

						@Override
						public void onAdFailedToLoad(AdRequestError adRequestError) {
							Utils.debug_log("banner:onAdFailedToLoad" + adRequestError.toString());
							dispatch_event("failedload" , "banner:"+ adRequestError.toString());
						}

						@Override
						public void onAdClicked() {
							Utils.debug_log("banner:onAdClicked");
							dispatch_event("clicked", "banner");
						}

						@Override
						public void onLeftApplication() {

						}

						@Override
						public void onReturnedToApplication() {

						}

						@Override
						public void onImpression(@Nullable ImpressionData impressionData) {
							Utils.debug_log("banner:onImpression");
							dispatch_event("impression", "banner");
						}
					});

					// Загрузка объявления.
					mBannerAdView.loadAd(adRequest);
				}
			}
		});
		return 0;

	}

	// yandexads.is_loaded(type)
	private int is_loaded(long L) {
		Utils.debug_log("is_loaded()");
		Utils.check_arg_count(L, 1);

		if (!check_is_initialized()) {
			return 0;
		}
		if (Lua.type(L, 1) != Lua.Type.STRING)
			return 0;

		final String type = Lua.tostring(L, 1);

		if (type.equals("interstitial")) {
			Lua.pushboolean(L, interstitial_ad_is_loaded);
			return 1;
		} else if (type.equals("rewarded")) {
			Lua.pushboolean(L, rewarded_video_ad_is_loaded);
			return 1;
		} else if (type.equals("banner")) {
			Lua.pushboolean(L, banner_is_loaded);
			return 1;
		}

		return 0;
	}

	// yandexads.show(type)
	private int show(long L) {
		Utils.debug_log("show()");
		Utils.check_arg_count(L, 1);

		if (!check_is_initialized()) {
			return 0;
		}
		if (Lua.type(L, 1) != Lua.Type.STRING)
			return 0;

		final String type = Lua.tostring(L, 1);

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (type.equals("interstitial")) {
					if (mInterstitialAd != null && mInterstitialAd.isLoaded()){
						mInterstitialAd.show();

					}
				} else if (type.equals("rewarded")) {
					if (mRewardedAd != null && mRewardedAd.isLoaded())
						mRewardedAd.show();
				}
			}
		});

		return 0;
	}

	public boolean isBannerLoaded() {
        return mBannerAdView != null && banner_is_loaded;
    }

	private void showBannerUiThread() {
		recreateBannerLayout();
		mBannerLayout.setVisibility(View.VISIBLE);
		mBannerAdView.setBackgroundColor(Color.TRANSPARENT);
	}
	

	private void destroyBannerUiThread() {
        if (!isBannerLoaded()) {
            return;
        }
        mBannerAdView.destroy();
        mBannerAdView = null;
        removeBannerLayout();
		banner_is_loaded = false;
    }

	private void removeBannerLayout() {
		if (mBannerLayout != null) {
			mBannerLayout.removeAllViews();
			activity.getWindowManager().removeView(mBannerLayout);
			mBannerLayout = null;
		}
	}

	private WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams();
        windowParams.x = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.y = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        windowParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        return windowParams;
    }

	private void recreateBannerLayout() {
		removeBannerLayout();
		mBannerLayout = new RelativeLayout(activity);
		mBannerLayout.setVisibility(View.GONE);
		mBannerLayout.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		adParams.setMargins(0, 0, 0, 0);
		mBannerLayout.addView(mBannerAdView, adParams);
		activity.getWindowManager().addView(mBannerLayout, getWindowLayoutParams());
	}


	private void dispatch_event(String phase, String event_type) {
		dispatch_eventEx(phase, event_type);
	}

	private void dispatch_eventEx(String phase, String event_type) {
		Hashtable<Object, Object> event = Utils.new_event("yandexads");
		event.put("phase", phase);
		event.put("type", event_type);
		Utils.dispatch_event(script_listener, event);
	}

}
