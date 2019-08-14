package gitReleaseNotes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to represent a collection of commits.
 * 
 * @author yesly
 *
 */
class Commits {
	List<Commit> listCommits = new ArrayList<Commit>();
	String repoName;

	public Commits(final String repoName) {
		this.repoName = repoName;
	}

	void addCommit(final Commit c) {
		listCommits.add(c);
	}

	public String toString(boolean byModules) {
		String s = repoName + ":\n";
		if (byModules) {
			return s + toStringByModules();
		}
		return s + toStringNotByModule(byModules);
	}

	public String toStringNotByModule(boolean byModules) {
		String s = "";
		final Iterator<Commit> it = listCommits.iterator();
		while (it.hasNext()) {
			s += it.next().toString(byModules);
		}
		return s;
	}

	public String toStringByModules() {
		Map<String, Commits> map = new TreeMap<String, Commits>();

		final Iterator<Commit> commitsIterator = listCommits.iterator();
		while (commitsIterator.hasNext()) {
			Commit c = commitsIterator.next();

			final Iterator<String> modulesIterator = c.getModules().iterator();
			while (modulesIterator.hasNext()) {
				String module = modulesIterator.next();
				Commits com;
				if (map.containsKey(module)) {
					com = map.get(module);
					com.addCommit(c);
				} else {
					com = new Commits(repoName);
					com.addCommit(c);
				}
				map.put(module, com);
			}
		}
		String s = "";
		for (Map.Entry<String, Commits> entry : map.entrySet()) {
			s += "\t" + entry.getKey() + ": \n" + entry.getValue().toStringNotByModule(true);
		}
		return s;
	}
}
