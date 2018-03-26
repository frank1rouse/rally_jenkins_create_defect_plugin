package com.vce.loadbuild.jenkins.plugins.rally;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.IOException;
import java.net.URI;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.QueryFilter;


/**
 *
 * @author Frank Rouse
 */
public class CreateRallyDefect extends Notifier {


    private final static String RALLY_URL = RallyUtils.RALLY_URL;


    // The defect fields
    private final String Project;
    private final String Priority;
    private final String Severity;
    private final String submittedBy;
    private final String defectCategory;
    private final String defectType;
    private final String foundInVersion;
    private final String whereFound;
    private final String whereIntroduced;
    private final String methodToIdentifySimilarDefects;
    private final String titlePrefix;

    // Boolean to determine if we create a defect if just unstable, default is false.
    private final boolean createDefectIfUnstable;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public CreateRallyDefect(boolean createDefectIfUnstable,
                             String project,
                             String priority,
                             String severity,
                             String submittedBy,
                             String defectCategory,
                             String defectType,
                             String foundInVersion,
                             String whereFound,
                             String whereIntroduced,
                             String methodToIdentifySimilarDefects,
                             String titlePrefix) {
        this.Project         = project;
        this.Priority        = priority;
        this.Severity        = severity;
        this.submittedBy     = submittedBy;
        this.defectCategory  = defectCategory;
        this.defectType      = defectType;
        this.foundInVersion  = foundInVersion;
        this.whereFound      = whereFound;
        this.whereIntroduced = whereIntroduced;
        this.methodToIdentifySimilarDefects = methodToIdentifySimilarDefects;
        this.titlePrefix     = titlePrefix;
        this.createDefectIfUnstable = createDefectIfUnstable;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getcreateDefectIfUnstable() {
        return createDefectIfUnstable;
    }

    public String getProject() {
        return Project;
    }
    public String getPriority() {
        return Priority;
    }

    public String getSeverity() {
        return Severity;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public String getDefectCategory() {
        return defectCategory;
    }
    
    public String getDefectType() {
        return defectType;
    }

    public String getFoundInVersion() {
        return foundInVersion;
    }

    public String getWhereFound() {
        return whereFound;
    }

    public String getWhereIntroduced() {
        return whereIntroduced;
    }

    public String getmethodToIdentifySimilarDefects() {
        return methodToIdentifySimilarDefects;
    }

    public String getTitlePrefix() {
        return titlePrefix;
    }

    private String getRallyUserRef(String rallyUserID, RallyRestApi restApi) throws IOException {
        String user_ref = null;
        QueryRequest userInfo = new QueryRequest("user");
        userInfo.setQueryFilter(new QueryFilter("UserName", "=", rallyUserID));
        QueryResponse queryresponse = restApi.query(userInfo);
        if (queryresponse.wasSuccessful()) {
            JsonArray results = queryresponse.getResults();
            JsonObject user = results.get(0).getAsJsonObject();
            user_ref = user.get("_ref").getAsString();
        } else {
            throw new IOException("Unable to resolve username " + rallyUserID);
        }
        return user_ref;
    }

    private JsonObject getRallyTag(String tagText, RallyRestApi restApi) throws IOException {
        JsonObject tag = null;
        QueryRequest tagInfo = new QueryRequest("Tag");
        tagInfo.setQueryFilter(new QueryFilter("Name", "=", tagText));
        QueryResponse queryresponse = restApi.query(tagInfo);
        if (queryresponse.wasSuccessful()) {
            JsonArray results = queryresponse.getResults();
            tag = results.get(0).getAsJsonObject();
        } else {
            throw new IOException("Unable to resolve tag " + tagText);
        }
        return tag;
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

        if (build.getResult() == Result.FAILURE || (build.getResult() == Result.UNSTABLE && createDefectIfUnstable)) {

            try {
                listener.getLogger().println("01");
                StringBuilder Name = new StringBuilder();
                if (getTitlePrefix().trim().length() > 0)
                   Name.append(getTitlePrefix().trim() + " ");
                Name.append(build.getFullDisplayName().split(" ")[0] + " build " + Integer.toString(build.getNumber()) + " is at status " + build.getResult().toString());
                // The getAbsoluteURL is deprecated so that hudson internal software does not reference.
                // External software that cannot resolve relative references may utilize.
                // http://javadoc.jenkins-ci.org/hudson/model/Run.html#getAbsoluteUrl()
                @SuppressWarnings("deprecation")
                String Description = "<a href=\"" + build.getAbsoluteUrl() + "console\">Jenkins Log</a>";
                String workspaceRef = RallyUtils.getWorkspaceReference(getDescriptor().getRallyWorkspace(), getDescriptor().getRallyAPIKey());
                String projectRef = RallyUtils.getProjectReference(workspaceRef, getProject(), getDescriptor().getRallyAPIKey());
                listener.getLogger().println("... Defect will be created with the following values.");
                listener.getLogger().println("... Title                           = " + Name);
                listener.getLogger().println("... Description                     = " + Description);
                listener.getLogger().println("... Workspace                       = " + workspaceRef);
                listener.getLogger().println("... Project                         = " + projectRef);
                listener.getLogger().println("... Priority                        = " + getPriority());
                listener.getLogger().println("... Severity                        = " + getSeverity());
                listener.getLogger().println("... SubmittedBy                     = " + getSubmittedBy());
                listener.getLogger().println("... DefectCategory                  = " + getDefectCategory());
                listener.getLogger().println("... DefectType                      = " + getDefectType());
                listener.getLogger().println("... FoundInVersion                  = " + getFoundInVersion());
                listener.getLogger().println("... WhereFound                      = " + getWhereFound());
                listener.getLogger().println("... WhereIntroduced                 = " + getWhereIntroduced());
                listener.getLogger().println("... Methodtoidentifysimilardefects  = " + getmethodToIdentifySimilarDefects());
                listener.getLogger().println("... Status of Build                 = " + build.getResult().toString());
                listener.getLogger().println("... Create if unstable              = " + getcreateDefectIfUnstable());
//                listener.getLogger().println("Rally API Key                   = " + getDescriptor().getRallyAPIKey());
                
                RallyRestApi restApi = new RallyRestApi(new URI(RALLY_URL), getDescriptor().getRallyAPIKey());
                JsonObject newDefect = new JsonObject();
                newDefect.addProperty("Name",                             Name.toString());
                newDefect.addProperty("Description",                      Description);
                newDefect.addProperty("Workspace",                        workspaceRef);
                newDefect.addProperty("Project",                          projectRef);
                newDefect.addProperty("Priority",                         getPriority());
                newDefect.addProperty("Severity",                         getSeverity());
                newDefect.addProperty("SubmittedBy",                      getRallyUserRef(getSubmittedBy(), restApi));
                newDefect.addProperty("Owner",                            getRallyUserRef(getSubmittedBy(), restApi));
                newDefect.addProperty("Author",                           getRallyUserRef(getSubmittedBy(), restApi));
                newDefect.addProperty("c_DefectCategory",                 getDefectCategory());
                newDefect.addProperty("c_DefectType",                     getDefectType());
                newDefect.addProperty("c_FoundinVersion",                 getFoundInVersion());
                newDefect.addProperty("c_WhereFound",                     getWhereFound());
                newDefect.addProperty("c_WhereIntroduced",                getWhereIntroduced());
                newDefect.addProperty("c_Methodtoidentifysimilardefects", getmethodToIdentifySimilarDefects());
                CreateRequest createRequest = new CreateRequest("defect", newDefect);
                CreateResponse createResponse = restApi.create(createRequest);
                if (createResponse.wasSuccessful()) {
                    // Grab the URL reference to the defect
                    String defectReference = createResponse.getObject().get("_ref").getAsString();
                    // Grab the defect reference number
                    String defectNum = defectReference.split("/")[(defectReference.split("/").length - 1)];
                    // Grab the project number
                    String projectNum = projectRef.split("/")[(projectRef.split("/").length - 1)];
                    listener.getLogger().println("... Created new defect " + RallyUtils.RALLY_URL + "/#/" + projectNum + "/detail/defect/" + defectNum);
                    listener.getLogger().println("... ");
                    listener.getLogger().println("... Now to add the tags.");
                    JsonArray Tags = new JsonArray();
                    Tags.add(getRallyTag("BRM_Build_Failure", restApi));
                    JsonObject defectUpdate = new JsonObject();
                    defectUpdate.add("Tags", Tags);
                    UpdateRequest updateDefectRequest = new UpdateRequest(defectReference, defectUpdate);
                    UpdateResponse updateDefectResponse = restApi.update(updateDefectRequest);
                    if (updateDefectResponse.wasSuccessful()) {
                        listener.getLogger().println("... Tags added successfully.");
                    } else {
                        listener.getLogger().println("... Unable to tag defect with BRM_Build_Failure");
                        for (int i = 0; i < updateDefectResponse.getErrors().length; i++) {
                            listener.getLogger().println(updateDefectResponse.getErrors()[i]);
                        }                        
                    }
                } else {
                    listener.getLogger().println("... Unable to create defect");
                    for (int i = 0; i < createResponse.getErrors().length; i++) {
                        listener.getLogger().println(createResponse.getErrors()[i]);
                    }
                }
                restApi.close();
            } catch (Exception e) {
                listener.getLogger().println("... Exception when attempting to create defect");
                listener.getLogger().println("... " + e.getLocalizedMessage());
                listener.getLogger().println("... Ensure that the correct Rally API key is entered under the");
                listener.getLogger().println("... Manage Jenkins=>Configure System=>Create Rally Defect=>Rally API Key\" field.");
                listener.getLogger().println("...");
                listener.getLogger().println("... No defect created");
            }
        } else {
            listener.getLogger().println("... No defect created");
        }
        // Always return true so if something is wrong with this code it won't mark the build as a failure.
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public CreateRallyDefectDescriptor getDescriptor() {
        return (CreateRallyDefectDescriptor) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }
}