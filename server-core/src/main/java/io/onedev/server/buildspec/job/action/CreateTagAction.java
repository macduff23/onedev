package io.onedev.server.buildspec.job.action;

import java.util.List;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.OneDev;
import io.onedev.server.OneException;
import io.onedev.server.buildspec.job.Job;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.TagProtection;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.Multiline;

@Editable(name="Create tag", order=400)
public class CreateTagAction extends PostBuildAction {

	private static final long serialVersionUID = 1L;
	
	private String tagName;
	
	private String tagMessage;
	
	@Editable(order=1000, description="Specify name of the tag. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://code.onedev.io/projects/onedev-manual/blob/master/pages/variable-substitution.md' target='_blank' tabindex='-1'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	@Editable(order=1050, description="Optionally specify message of the tag. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://code.onedev.io/projects/onedev-manual/blob/master/pages/variable-substitution.md' target='_blank' tabindex='-1'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Multiline
	@Interpolative(variableSuggester="suggestVariables")
	public String getTagMessage() {
		return tagMessage;
	}

	public void setTagMessage(String tagMessage) {
		this.tagMessage = tagMessage;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return Job.suggestVariables(matchWith);
	}

	@Override
	public void execute(Build build) {
		PersonIdent tagIdent = OneDev.getInstance(UserManager.class).getSystem().asPerson();
		Project project = build.getProject();
		String tagName = getTagName();
		Ref tagRef = project.getTagRef(tagName);
		TagProtection protection = project.getTagProtection(tagName, build);
		if (tagRef != null) {
			if (protection.isPreventUpdate()) {
				throw new OneException("Updating tag '" + tagName + "' is not allowed in this build");
			} else {
				OneDev.getInstance(ProjectManager.class).deleteTag(project, tagName);
				project.createTag(tagName, build.getCommitHash(), tagIdent, getTagMessage());
			}
		} else if (protection.isPreventCreation()) {
			throw new OneException("Creating tag '" + tagName + "' is not allowed in this build");
		} else {
			project.createTag(tagName, build.getCommitHash(), tagIdent, getTagMessage());
		}
	}

	@Override
	public String getDescription() {
		return "Create tag";
	}

}
