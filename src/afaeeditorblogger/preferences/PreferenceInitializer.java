package afaeeditorblogger.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import afaeeditorblogger.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		store.setDefault(PreferenceConstants.P_BLOGGING_URL, "http://[your_site]/xmlrpc.php");
		store.setDefault(PreferenceConstants.P_BLOGGING_ID, "My Blog");
		store.setDefault(PreferenceConstants.P_BLOGGING_USERNAME, "admin");
		store.setDefault(PreferenceConstants.P_BLOGGING_PASSWORD, "");
		
		store.setDefault(PreferenceConstants.P_BLOGGING_WORKING_DIR, Activator.getDefault().getStateLocation().toOSString());
	}

}
