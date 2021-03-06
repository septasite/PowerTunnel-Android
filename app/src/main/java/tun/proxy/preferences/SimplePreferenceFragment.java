package tun.proxy.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import tun.proxy.MyApplication;
import tun.proxy.preferences.fragments.PackageListPreferenceFragment;
import tun.proxy.preferences.preference.EditTextSummaryPreference;

import static android.preference.Preference.OnPreferenceChangeListener;
import static android.preference.Preference.OnPreferenceClickListener;

public class SimplePreferenceFragment extends PreferenceFragment implements OnPreferenceClickListener {

    public static final String VPN_CONNECTION_MODE = "vpn_connection_mode";
    public static final String VPN_DISALLOWED_APPLICATION_LIST = "vpn_disallowed_application_list";
    public static final String VPN_ALLOWED_APPLICATION_LIST = "vpn_allowed_application_list";
    public static final String VPN_CLEAR_ALL_SELECTION = "vpn_clear_all_selection";
    public static final String DNS_PROVIDER = "dns_provider";
    public static final String SPECIFIED_DNS = "specified_dns_provider";
    public static final String RESET_CONNECTION_SETTINGS = "reset_connection_settings";
    public static final String PROXY_IP = "proxy_ip";
    public static final String PROXY_PORT = "proxy_port";

    private ListPreference prefPackage;
    private PreferenceScreen prefDisallow;
    private PreferenceScreen prefAllow;
    private PreferenceScreen clearAllSelection;

    private ListPreference prefDns;
    private EditTextSummaryPreference prefSpecDns;

    private SharedPreferences prefs;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(true);

        prefPackage = (ListPreference) this.findPreference(VPN_CONNECTION_MODE);
        prefDisallow = (PreferenceScreen) findPreference(VPN_DISALLOWED_APPLICATION_LIST);
        prefAllow = (PreferenceScreen) findPreference(VPN_ALLOWED_APPLICATION_LIST);

        clearAllSelection = (PreferenceScreen) findPreference(VPN_CLEAR_ALL_SELECTION);
        prefDisallow.setOnPreferenceClickListener(this);
        prefAllow.setOnPreferenceClickListener(this);
        clearAllSelection.setOnPreferenceClickListener(this);
        findPreference(RESET_CONNECTION_SETTINGS).setOnPreferenceClickListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        prefSpecDns = ((EditTextSummaryPreference) findPreference(SPECIFIED_DNS));

        prefDns = ((ListPreference) findPreference(DNS_PROVIDER));
        prefDns.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateSpecDnsStatus(((String) newValue));
                return true;
            }
        });

        prefPackage.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if (preference instanceof ListPreference) {
                    final ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue((String) value);
                    prefDisallow.setEnabled(index == MyApplication.VPNMode.DISALLOW.ordinal());
                    prefAllow.setEnabled(index == MyApplication.VPNMode.ALLOW.ordinal());
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                }
                return true;
            }
        });

        findPreference("theme").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                MainActivity.applyTheme(((String) newValue));
                return true;
            }
        });

        updateProxyVpn(!prefs.getBoolean("proxy_mode", false));
        findPreference("proxy_mode").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean vpnMode = !(boolean)newValue;
                if(!vpnMode && context != null) {
                    Toast.makeText(context, R.string.proxy_mode_warning, Toast.LENGTH_LONG).show();
                }
                updateProxyVpn(vpnMode);
                return true;
            }
        });

        updateSpecDnsStatus(prefs.getString(DNS_PROVIDER, "CLOUDFLARE"));
        updateMenuItem();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity.getApplicationContext();
    }

    private void updateProxyVpn(boolean vpnMode) {
        prefPackage.setEnabled(vpnMode);
        prefAllow.setEnabled(vpnMode);
        prefDisallow.setEnabled(vpnMode);
        clearAllSelection.setEnabled(vpnMode);
        updateMenuItem(vpnMode);
    }

    private void updateSpecDnsStatus(String provider) {
        prefSpecDns.setEnabled(provider.equals("SPECIFIED"));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMenuItem();
    }

    private void updateMenuItem() {
        updateMenuItem(!prefs.getBoolean("proxy_mode", false));
    }

    private void updateMenuItem(boolean vpnMode) {
        if(!vpnMode) {
            return;
        }
        int countDisallow = MyApplication.getInstance().loadVPNApplication(MyApplication.VPNMode.DISALLOW).size();
        prefDisallow.setTitle(getString(R.string.pref_disallowed_application_list) + (countDisallow > 0 ? " (" + countDisallow + ")" : ""));
        prefDisallow.setEnabled(MyApplication.VPNMode.DISALLOW.name().equals(prefPackage.getValue()));

        int countAllow = MyApplication.getInstance().loadVPNApplication(MyApplication.VPNMode.ALLOW).size();
        prefAllow.setTitle(getString(R.string.pref_allowed_application_list) + (countAllow > 0 ? " (" + countAllow + ")" : ""));
        prefAllow.setEnabled(MyApplication.VPNMode.ALLOW.name().equals(prefPackage.getValue()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case VPN_DISALLOWED_APPLICATION_LIST: {
                transitionFragment(PackageListPreferenceFragment.newInstance(MyApplication.VPNMode.DISALLOW));
                break;
            }
            case VPN_ALLOWED_APPLICATION_LIST: {
                transitionFragment(PackageListPreferenceFragment.newInstance(MyApplication.VPNMode.ALLOW));
                break;
            }
            case VPN_CLEAR_ALL_SELECTION: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.pref_clear_all))
                        .setMessage(getString(R.string.pref_dialog_clear_all))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Set<String> emptySet = new HashSet<>();
                                MyApplication.getInstance().storeVPNApplication(MyApplication.VPNMode.ALLOW, emptySet);
                                MyApplication.getInstance().storeVPNApplication(MyApplication.VPNMode.DISALLOW, emptySet);
                                updateMenuItem();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            }
            case RESET_CONNECTION_SETTINGS: {
                ((EditTextSummaryPreference) findPreference(PROXY_IP)).setText(getString(R.string.proxy_ip));
                ((EditTextSummaryPreference) findPreference(PROXY_PORT)).setText(getString(R.string.proxy_port));
                break;
            }
        }
        return false;
    }

    private void transitionFragment(PreferenceFragment nextPreferenceFragment) {
        getFragmentManager().beginTransaction().addToBackStack(null)
                .replace(android.R.id.content, nextPreferenceFragment).commit();
    }
}
