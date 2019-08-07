package yesly;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

	public GetGitCommits(final String pathToRepo) {
		this.pathToRepo = pathToRepo;
	}

	/**
	 * Builds the release notes/report for the repository and it's Git sub modules.
	 * 
	 * @throws IOException
	 * @throws GitAPIException
	 */
	public void buildReport() throws IOException, GitAPIException {
		final PrintWriter writer = new PrintWriter("commits-report.txt", "UTF-8");
		writer.println("Commits Since Last Tagged Commit:");

		// Get commits for original repository.
		Repository repo = new FileRepository(pathToRepo + ".git");
		writer.println(getCommits(writer, repo, null).toString());

		// Get commits for Git sub modules.
		final SubmoduleWalk walk = SubmoduleWalk.forIndex(repo);
		while (walk.next()) {
			repo = walk.getRepository();
			writer.println(getCommits(writer, repo, walk).toString());
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
	Set<String> modifiedFiles = new HashSet<String>();

	Commit(final String message, final String author, final Date date, final Set<String> modifiedFiles) {
		this.message = message;
		this.author = author;
		this.date = date;
		this.modifiedFiles.addAll(modifiedFiles);
	}

	@Override
	public String toString() {
		return "Author: " + this.author + " - Date: " + this.date + " - File(s): " + this.modifiedFiles + " - Message: "
				+ this.message;
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

	@Override
	public String toString() {
		String s = repoName + ":\n";
		final Iterator<Commit> it = listCommits.iterator();
		while (it.hasNext()) {
			s = s + it.next().toString();
		}
		return s;
	}
}