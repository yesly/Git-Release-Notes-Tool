package gitReleaseNotes;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

public class GetGitCommits {
	private final String pathToRepo;
	private final String reportName;
	private final boolean gitSubModules;
	private final boolean byModules;
	private final boolean allCommits; // default is since last tagged commit
	private final boolean byVersion;

	public GetGitCommits(final String pathToRepo, final String reportName, final boolean gitSubModules, final boolean byModules, final boolean allCommits, final boolean byVersion)
			throws IOException, GitAPIException {
		this.pathToRepo = pathToRepo;
		this.reportName = reportName;
		this.gitSubModules = gitSubModules;
		this.byModules = byModules;
		this.allCommits = allCommits;
		this.byVersion = byVersion;
		buildReport();
	}

	/**
	 * Builds the release notes/report for the repository and it's Git sub modules.
	 * 
	 * @throws IOException
	 * @throws GitAPIException
	 */
	private void buildReport() throws IOException, GitAPIException {
		BasicConfigurator.configure();
		final PrintWriter writer = new PrintWriter(reportName + ".txt", "UTF-8");
		if(allCommits) {
			writer.println("All commits:");
		} else {
			writer.println("Commits Since Last Tagged Commit:");
		}

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
		
		Iterable<RevCommit> commitsIter = git.log().addRange(lastTaggedCommit, headCommit).call();
		if (allCommits) {
			commitsIter = git.log().all().call();
		}
		
		for (final RevCommit commit : commitsIter) {
			final Set<String> modifiedFiles = getFilesForCommit(repo, commit);
			String message = commit.getFullMessage();
			message = message.replace("\n", " ");
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
	 * @throws CorruptObjectException 
	 * @throws IncorrectObjectTypeException 
	 * @throws MissingObjectException 
	 * @throws IOException
	 */
	private static Set<String> getFilesForCommit(final Repository repo, final RevCommit commit) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		final Set<String> files = new HashSet<String>();
		
		final DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setRepository(repo);
		df.setDetectRenames(true);
		
		List<DiffEntry> diffs;
		try {
			diffs = df.scan(commit.getParent(0).getTree(), commit.getTree());  // Get differences between this and previous commit
			
			for (final DiffEntry diff : diffs) { // Add differences to set.
				files.add(diff.getNewPath());
			}

			df.close();
			
		} catch (ArrayIndexOutOfBoundsException e) {
			ObjectId treeId = commit.getTree();
			TreeWalk treeWalk = new TreeWalk(repo);
			treeWalk.reset(treeId);
			
			while (treeWalk.next()) {
			    files.add(treeWalk.getPathString());
			}
			
			treeWalk.close();
			
			return files;
		}

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