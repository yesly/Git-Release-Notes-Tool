package yesly;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

public class GetGitCommits {
	private final String pathToRepo;
	private final String reportName;
	private final boolean gitSubModules;
	private final boolean byModules;

	public GetGitCommits(final String pathToRepo, final String reportName, final boolean gitSubModules, final boolean byModules)
			throws IOException, GitAPIException {
		this.pathToRepo = pathToRepo;
		this.reportName = reportName;
		this.gitSubModules = gitSubModules;
		this.byModules = byModules;
		buildReport();
	}

	/**
	 * Builds the release notes/report for the repository and it's Git sub modules.
	 * 
	 * @throws IOException
	 * @throws GitAPIException
	 */
	public void buildReport() throws IOException, GitAPIException {
		BasicConfigurator.configure();
		final PrintWriter writer = new PrintWriter(reportName + ".txt", "UTF-8");
		writer.println("Commits Since Last Tagged Commit:");

		Repository repo = new FileRepository(pathToRepo + ".git");
		writer.println(getCommits(writer, repo, null).toString(byModules));

		if (gitSubModules) {
			final SubmoduleWalk walk = SubmoduleWalk.forIndex(repo);
			while (walk.next()) {
				repo = walk.getRepository();
				writer.println(getCommits(writer, repo, walk).toString(byModules));

			}
		}

		writer.close();
	}

	/**
	 * Gets the list of commits for the given repository.
	 * 
	 * @param writer to write to .txt file
	 * @param repo
	 * @param walk   if null not a sub module
	 * @return Commits for the given repository
	 * @throws GitAPIException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	private Commits getCommits(final PrintWriter writer, final Repository repo, final SubmoduleWalk walk)
			throws GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		final Git git = new Git(repo);
		final ObjectId lastTaggedCommit = getLastTaggedCommit(repo, git);
		final ObjectId headCommit = getHeadCommitId(git);
		if (lastTaggedCommit.equals(new ObjectId(0, 0, 0, 0, 0)) || headCommit == null) { // Check for invalid range.
			return null;
		}

		String repoName;
		if (walk != null) {
			repoName = walk.getModuleName();
		} else {
			final String segments[] = pathToRepo.split("/");
			repoName = segments[segments.length - 1];
		}

		final Commits com = new Commits(repoName);
		final Iterable<RevCommit> commits = git.log().addRange(lastTaggedCommit, headCommit).call();
		for (final RevCommit commit : commits) {
			final Set<String> modifiedFiles = getFilesForCommit(repo, commit);
			final String message = commit.getFullMessage();
			final String author = commit.getAuthorIdent().getName();
			final Date date = new Date(commit.getCommitTime() * 1000L);
			com.addCommit(new Commit(message, author, date, modifiedFiles));
		}

		git.close();

		return com;
	}

	/**
	 * Gets the set of modified files by the given commit.
	 * 
	 * @param repo
	 * @param commit
	 * @return set of modified files
	 * @throws IOException
	 */
	private static Set<String> getFilesForCommit(final Repository repo, final RevCommit commit) throws IOException {
		final Set<String> files = new HashSet<String>();

		final DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setRepository(repo);
		df.setDetectRenames(true);

		final List<DiffEntry> diffs = df.scan(commit.getParent(0).getTree(), commit.getTree()); // Get differences
																								// between this and the
																								// previous commit.

		for (final DiffEntry diff : diffs) { // Add differences to set.
			files.add(diff.getNewPath());
		}

		df.close();

		return files;
	}

	/**
	 * Gets the last tagged commit's object id. Checks for lightweight and annotated
	 * tags.
	 * 
	 * @param repo
	 * @param git
	 * @return last tagged commit
	 * @throws GitAPIException
	 */
	private ObjectId getLastTaggedCommit(final Repository repo, final Git git) throws GitAPIException {
		final List<Ref> list = git.tagList().call();

		if (list.isEmpty()) {
			return new ObjectId(0, 0, 0, 0, 0);
		}

		return getActualRefObjectId(repo, list.get(list.size() - 1));
	}

	/**
	 * Gets the last commit in a repository.
	 * 
	 * @param git
	 * @return last commit
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	private ObjectId getHeadCommitId(final Git git)
			throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		return git.getRepository().resolve("HEAD");
	}

	@SuppressWarnings("deprecation")
	private static ObjectId getActualRefObjectId(final Repository repo, final Ref ref) {
		final Ref repoPeeled = repo.peel(ref);
		if (repoPeeled.getPeeledObjectId() != null) {
			return repoPeeled.getPeeledObjectId();
		}
		return ref.getObjectId();
	}
}

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
		return "\t\t" + this.author + " - " + this.date + files + " - " + this.message;
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