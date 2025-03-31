package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.ContactEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ContactDaoTest {
    private lateinit var contactDao: ContactDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        contactDao = db.contactDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_getByEmail_returns_correctly_when_add_new_contact`() = runTest {
        val contact = ContactEntity(
            handle = "handle",
            mail = "lh@mega.co.nz",
            nickName = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        contactDao.insertOrUpdateContact(contact)
        val actual = contactDao.getContactByEmail("lh@mega.co.nz")
        assertThat(actual?.handle).isEqualTo(contact.handle)
        assertThat(actual?.mail).isEqualTo(contact.mail)
        assertThat(actual?.nickName).isEqualTo(contact.nickName)
        assertThat(actual?.firstName).isEqualTo(contact.firstName)
        assertThat(actual?.lastName).isEqualTo(contact.lastName)
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_getByHandle_returns_correctly_when_add_new_contact`() = runTest {
        val contact = ContactEntity(
            handle = "handle",
            mail = "lh@mega.co.nz",
            nickName = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        contactDao.insertOrUpdateContact(contact)
        val actual = contactDao.getContactByHandle("handle")
        assertThat(actual?.handle).isEqualTo(contact.handle)
        assertThat(actual?.mail).isEqualTo(contact.mail)
        assertThat(actual?.nickName).isEqualTo(contact.nickName)
        assertThat(actual?.firstName).isEqualTo(contact.firstName)
        assertThat(actual?.lastName).isEqualTo(contact.lastName)
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_getAll_returns_correctly_when_add_list_of_contact`() = runTest {
        val contacts = (1..10).map {
            val contact = ContactEntity(
                handle = "handle${it}",
                mail = "lh@mega.co.nz${it}",
                nickName = "Jayce${it}",
                firstName = "Hai${it}",
                lastName = "Luong${it}"
            )
            contactDao.insertOrUpdateContact(contact)
            contact
        }
        contactDao.getAllContact().first().forEachIndexed { i, entity ->
            assertThat(entity.handle).isEqualTo(contacts[i].handle)
            assertThat(entity.mail).isEqualTo(contacts[i].mail)
            assertThat(entity.nickName).isEqualTo(contacts[i].nickName)
            assertThat(entity.firstName).isEqualTo(contacts[i].firstName)
            assertThat(entity.lastName).isEqualTo(contacts[i].lastName)
        }
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_table_empty_when_call_deleteAll`() = runTest {
        (1..10).forEach {
            val contact = ContactEntity(
                handle = "handle${it}",
                mail = "lh@mega.co.nz${it}",
                nickName = "Jayce${it}",
                firstName = "Hai${it}",
                lastName = "Luong${it}"
            )
            contactDao.insertOrUpdateContact(contact)
        }
        contactDao.deleteAllContact()
        assertThat(contactDao.getAllContact().first().size).isEqualTo(0)
    }

    @Test
    fun test_that_null_is_returned_if_no_user_exists() = runTest {
        contactDao.monitorContactByEmail("email.nonexists.com").test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun test_that_updates_to_a_contact_is_reflected_in_monitor_contact() = runTest {
        val handle = "handle"
        val contact = ContactEntity(
            handle = handle,
            mail = "lh@mega.co.nz",
            nickName = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        contactDao.insertOrUpdateContact(contact)
        val expectedNickname = "UberDevMeister"

        contactDao.monitorContactByHandle(handle).test {
            assertThat(awaitItem()).isEqualTo(contact)
            contactDao.insertOrUpdateContact(contact.copy(nickName = expectedNickname))
            assertThat(awaitItem().nickName).isEqualTo(expectedNickname)
        }
    }

    @Test
    fun test_that_updates_to_a_contact_is_reflected_in_monitor_contact_by_email() = runTest {
        val handle = "handle"
        val mail = "lh@mega.co.nz"
        val contact = ContactEntity(
            handle = handle,
            mail = mail,
            nickName = "Jayce",
            firstName = "Hai",
            lastName = "Luong"
        )
        contactDao.insertOrUpdateContact(contact)
        val expectedNickname = "UberDevMeister"

        contactDao.monitorContactByEmail(mail).test {
            assertThat(awaitItem()).isEqualTo(contact)
            contactDao.insertOrUpdateContact(contact.copy(nickName = expectedNickname))
            assertThat(awaitItem()?.nickName).isEqualTo(expectedNickname)
        }
    }
}