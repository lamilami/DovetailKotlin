package bg.workshop.dovetail.model

import bg.workshop.dovetail.geometry.JointType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private fun project(id: String, updated: Long) =
        Project(id, "P$id", JointType.DOVETAIL, mapOf("THICKNESS" to "18"), createdAt = 0, updatedAt = updated)

    @Test fun roundTripsThroughJson() {
        val lib = Library.seed().upsert(project("a", 5))
        assertEquals(lib, LibraryCodec.decode(LibraryCodec.encode(lib)))
    }

    @Test fun ignoresUnknownKeys() {
        val text = """{"version":1,"jigs":[{"id":"j","name":"J","finger":12.7,"slot":12.7,"future":true}],"extra":1}"""
        assertEquals(Jig("j", "J", 12.7, 12.7), LibraryCodec.decode(text).jigs.single())
    }

    @Test fun upsertReplacesById() {
        val lib = Library().upsert(Jig("j", "A", 1.0, 1.0)).upsert(Jig("j", "B", 2.0, 2.0))
        assertEquals(listOf(Jig("j", "B", 2.0, 2.0)), lib.jigs)
    }

    @Test fun upsertAppendsNew() {
        val lib = Library().upsert(Jig("a", "A", 1.0, 1.0)).upsert(Jig("b", "B", 2.0, 2.0))
        assertEquals(listOf("a", "b"), lib.jigs.map { it.id })
    }

    @Test fun deletesProject() {
        val lib = Library().upsert(project("a", 1)).upsert(project("b", 2)).deleteProject("a")
        assertNull(lib.project("a"))
        assertEquals("b", lib.projects.single().id)
    }

    @Test fun recentProjectsNewestFirst() {
        val lib = Library().upsert(project("old", 1)).upsert(project("new", 9)).upsert(project("mid", 5))
        assertEquals(listOf("new", "mid", "old"), lib.recentProjects().map { it.id })
    }

    @Test fun bitsFilteredByJoint() {
        val seed = Library.seed()
        assertTrue(seed.bitsFor(JointType.DOVETAIL).all { it.kind == BitKind.DOVETAIL })
        assertEquals(listOf("bit-st-12-7"), seed.bitsFor(JointType.FINGER).map { it.id })
    }

    @Test fun seedHasThreeStandardJigs() {
        assertEquals(listOf("jig-12-7", "jig-7-16", "jig-9-16"), Library.seed().jigs.map { it.id })
    }

    @Test fun migrateAddsMissingPresetsAndKeepsUserData() {
        val mine = Jig("jig-12-7", "Моят", 12.7, 12.7)
        val custom = Jig("x", "Друг", 10.0, 10.0)
        val old = Library(version = 1, jigs = listOf(mine, custom))
        val m = old.migrate()
        assertEquals(Library.CURRENT_VERSION, m.version)
        assertEquals(listOf("jig-12-7", "x", "jig-7-16", "jig-9-16"), m.jigs.map { it.id })
        assertEquals("Моят", m.jigs.first().name)
    }

    @Test fun migrateIsNoOpOnCurrentVersion() {
        val lib = Library.seed().deleteJig("jig-7-16")
        assertEquals(lib, lib.migrate())
    }
}
