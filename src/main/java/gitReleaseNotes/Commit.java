package gitReleaseNotes;

import java.sql.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class to represent a commit.
 * 
 * @author yesly
 * 
 */
class Commit {
	String message;
	String author;
	Date date;
	Set<String> modules = new HashSet<String>();

	Commit(final String message, final String author, final Date date, final Set<String> modifiedFiles) {
		this.message = message;
		this.author = author;
		this.date = date;
		this.modules.addAll(getModules(modifiedFiles));
	}

	public String toString(final boolean byModules) {
		String files = "";
		if (!byModules) {
			files = " - " + modules;
		}
		
		return "\t\t" + this.author + " - " + this.date + files + " - " + this.message + "\n";
	}

	private Set<String> getModules(final Set<String> files) {
		final Set<String> modules = new HashSet<String>();
		final Iterator<String> it = files.iterator();
		while (it.hasNext()) {
			modules.add(it.next().split("/")[0]);
		}
		return modules;
	}

	public Set<String> getModules() {
		return modules;
	}
}
