package com.vce.loadbuild.jenkins.plugins.rally;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.gson.JsonArray;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;


@Extension // This indicates to Jenkins that this is an implementation of an extension point.
public final class CreateRallyDefectDescriptor extends BuildStepDescriptor<Publisher> {
    /**
     * To persist global configuration information,
     * simply store it in a field and call save().
     *
     * <p>
     * If you don't want fields to be persisted, use <tt>transient</tt>.
     */

    // Local holder of the API key.
    private String RallyAPIKey;

    // Local holder of the Workspace
    private String RallyWorkspace;

    /**
     * In order to load the persisted global configuration, you have to 
     * call load() in the constructor.
     */
    public CreateRallyDefectDescriptor() {
        super(CreateRallyDefect.class);
        load();
    }


    /**
     * Performs on-the-fly validation of the form field 'name'.
     *
     * @param value
     *      This parameter receives the value that the user has typed.
     * @return
     *      Indicates the outcome of the validation. This is sent to the browser.
     *      <p>
     *      Note that returning {@link FormValidation#error(String)} does not
     *      prevent the form from being saved. It just means that a message
     *      will be displayed to the user. 
     * @throws URISyntaxException 
     */
    public FormValidation doCheckSubmittedBy(@QueryParameter String value) {
        QueryResponse queryresponse = null;
        try {
            if (value.length() == 0)
                return FormValidation.error("Please set a rally user id.");
            else if (value.length() < 4)
                return FormValidation.warning("Isn't the user id too short?");
            RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RallyAPIKey);
            restApi.setApplicationName("TestDefectCreation");
            QueryRequest userInfo = new QueryRequest("user");
            userInfo.setQueryFilter(new QueryFilter("UserName", "=", value));
            queryresponse = restApi.query(userInfo);
            // Close the restApi regardless of the outcome of the following tests.
            restApi.close();
        } catch (Exception e) {
            return FormValidation.error("Exception when attempting to validate rally user id.\n" +
                    e.getLocalizedMessage() + "\nEnsure that the correct Rally API key is entered under the\n\"Manage Jenkins=>Configure System=>Create Rally Defect=>Rally API Key\" field.");
        }
        if (queryresponse.wasSuccessful()) {
            JsonArray results = queryresponse.getResults();
            if (results.size() > 0)
                return FormValidation.ok();
        }
        return FormValidation.error("Unable to validate rally user id \"" + value + "\"");
    }


    /**
     * doFillXXXXXXItems
     * The XXXXXX is taken from the config.jelly fentry field name where
     * the first letter is capitalized to reflect camel case.
     * @return
     * @throws IOException 
     * @throws URISyntaxException 
     * While you can set the default value for the drop down here if you do the default will be recalculated every
     * time the config window is open. If you would like to preserve previous selections it is better to put the
     * default value in the config.jelly file as this is only read once and will therefore preserve previous
     * modifications.
     */
    public ListBoxModel doFillProjectItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.listAllRallyProjects(workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }

    public ListBoxModel doFillPriorityItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Priority", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }

    public ListBoxModel doFillSeverityItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Severity", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }


    public ListBoxModel doFillDefectCategoryItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Defect Category", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }


    public ListBoxModel doFillDefectTypeItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Defect Type", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }


    public ListBoxModel doFillFoundInVersionItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Found in Version", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }


    public ListBoxModel doFillWhereFoundItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Where Found", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }


    public ListBoxModel doFillWhereIntroducedItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Where Introduced?", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }


    public ListBoxModel doFillMethodToIdentifySimilarDefectsItems() throws URISyntaxException, IOException {
        ListBoxModel items = new ListBoxModel();
        String workspaceRef = RallyUtils.getWorkspaceReference(RallyWorkspace, RallyAPIKey);
        ArrayList<String> fieldValues = RallyUtils.allowedFieldValues("Defect", "Method to identify similar defects", workspaceRef, RallyAPIKey);
        for (String tempString: fieldValues)
            items.add(tempString, tempString);
        return items;
    }

    public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
        // Indicates that this builder can be used with all kinds of project types 
        return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName() {
        return "Create Rally Defect";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        // To persist global configuration information,
        // set that to properties and call save().
        RallyAPIKey    = formData.getString("rallyAPIKey");
        RallyWorkspace = formData.getString("rallyWorkspace");
        save();
        return super.configure(req,formData);
    }

    /**
     * This method returns true if the global configuration says we should speak French.
     *
     * The method name is bit awkward because global.jelly calls this method to determine
     * the initial state of the checkbox by the naming convention.
     */
    public String getRallyAPIKey() {
        return RallyAPIKey;
    }

    public String getRallyWorkspace() {
        return RallyWorkspace;
    }
}