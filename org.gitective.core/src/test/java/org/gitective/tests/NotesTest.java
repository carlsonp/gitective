/*
 * Copyright (c) 2011 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package org.gitective.tests;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepository;
import org.gitective.core.GitException;
import org.gitective.core.filter.commit.AndCommitFilter;
import org.gitective.core.filter.commit.CommitCountFilter;
import org.gitective.core.filter.commit.CommitNotesFilter;
import org.gitective.core.service.CommitFinder;
import org.junit.Test;

/**
 * Unit tests of {@link CommitNotesFilter}
 */
public class NotesTest extends GitTestCase {

	/**
	 * Test getting content of note
	 * 
	 * @throws Exception
	 */
	@Test
	public void noteContentCallback() throws Exception {
		add("test.txt", "abc");
		final String note = "this is a note";
		note(note);

		final AtomicReference<String> found = new AtomicReference<String>();

		CommitFinder finder = new CommitFinder(testRepo);
		finder.setFilter(new CommitNotesFilter() {

			protected boolean include(RevCommit commit, Note note,
					String content) {
				found.set(content);
				return super.include(commit, note, content);
			}

		});
		finder.find();
		assertEquals(note, found.get());
	}

	/**
	 * Test not including a commit based on the note found
	 * 
	 * @throws Exception
	 */
	@Test
	public void noteNotIncluded() throws Exception {
		add("test.txt", "abc");
		note("this is a note");

		CommitCountFilter count = new CommitCountFilter();
		CommitFinder finder = new CommitFinder(testRepo);
		finder.setFilter(new AndCommitFilter(new CommitNotesFilter() {

			protected boolean include(RevCommit commit, Note note,
					String content) {
				return false;
			}

		}, count)).find();
		assertEquals(0, count.getCount());
	}

	/**
	 * Unit test of
	 * {@link CommitNotesFilter#setRepository(org.eclipse.jgit.lib.Repository)}
	 */
	@Test
	public void setNoRepository() {
		CommitNotesFilter filter = new CommitNotesFilter();
		assertSame(filter, filter.setRepository(null));
	}

	private static class BadRefDatabase extends RefDatabase {

		private final IOException exception;

		public BadRefDatabase(IOException exception) {
			this.exception = exception;
		}

		public void create() throws IOException {
			throw exception;
		}

		public void close() {
		}

		public boolean isNameConflicting(String name) throws IOException {
			throw exception;
		}

		public RefUpdate newUpdate(String name, boolean detach)
				throws IOException {
			throw exception;
		}

		public RefRename newRename(String fromName, String toName)
				throws IOException {
			throw exception;
		}

		public Ref getRef(String name) throws IOException {
			throw exception;
		}

		public Map<String, Ref> getRefs(String prefix) throws IOException {
			throw exception;
		}

		public List<Ref> getAdditionalRefs() throws IOException {
			throw exception;
		}

		public Ref peel(Ref ref) throws IOException {
			throw exception;
		}
	}

	/**
	 * Set invalid repository on notes filter
	 * 
	 * @throws Exception
	 */
	@Test
	public void setRepositoryThrowsIOException() throws Exception {
		CommitNotesFilter filter = new CommitNotesFilter();
		final IOException exception = new IOException("message");
		Repository repo = new FileRepository(testRepo) {

			public RefDatabase getRefDatabase() {
				return new BadRefDatabase(exception);
			}

		};
		try {
			filter.setRepository(repo);
			fail("Exception not thrown when reading bad refs");
		} catch (GitException e) {
			assertNotNull(e);
			assertEquals(exception, e.getCause());
		}
	}

	/**
	 * Unit test of {@link CommitNotesFilter#clone()}
	 * 
	 * @throws Exception
	 */
	@Test
	public void cloneFilter() throws Exception {
		CommitNotesFilter filter = new CommitNotesFilter();
		RevFilter clone = filter.clone();
		assertNotNull(clone);
		assertNotSame(filter, clone);
		assertTrue(clone instanceof CommitNotesFilter);
	}
}
