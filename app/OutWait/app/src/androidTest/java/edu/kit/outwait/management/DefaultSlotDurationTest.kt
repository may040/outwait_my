package edu.kit.outwait.management

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import edu.kit.outwait.MainActivity
import edu.kit.outwait.R
import edu.kit.outwait.dataItem.TimeSlotItem
import edu.kit.outwait.instituteRepository.InstituteRepository
import edu.kit.outwait.recyclerviewSetUp.viewHolder.BaseViewHolder
import edu.kit.outwait.util.*
import edu.kit.outwait.utils.EspressoIdlingResource
import edu.kit.outwait.waitingQueue.timeSlotModel.ClientTimeSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DefaultSlotDurationTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var openActivityRule = activityScenarioRule<MainActivity>()

    @Inject
    lateinit var instituteRepo: InstituteRepository

    @Before
    fun initTest() {
        hiltRule.inject()
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        instituteRepo.login(VALID_TEST_USERNAME, VALID_TEST_PASSWORD)

        clearQueue()
    }

    fun clearQueue() {
        Thread.sleep(WAIT_FOR_UI_RESPONSE)
        val timeSlots = instituteRepo.getObservableTimeSlotList().value

        if (timeSlots != null && timeSlots.isNotEmpty()) {
            for (slot in timeSlots.filterIsInstance<ClientTimeSlot>()) {
                // Delete slot with retrieved slotCode from waiting queue.
                instituteRepo.deleteSlot(slot.slotCode)
                Thread.sleep(WAIT_RESPONSE_SERVER_LONG)
            }
            // Save the transaction and the changes made.
            CoroutineScope(Dispatchers.Main).launch {
                instituteRepo.saveTransaction()
            }
        }
    }

    // T13
    @Test
    fun defaultSlotDuration() {
        // Check Management view
        onView(withId(R.id.floatingActionButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        // Set default duration
        onView(withId(R.id.config)).perform(click())
        DigitSelector.pressClear(R.id.configStandardDuration)
        DigitSelector.pressDigit(DigitSelector.digitFour, R.id.configStandardDuration)
        DigitSelector.pressDigit(DigitSelector.digitTwo, R.id.configStandardDuration)
        onView(withId(R.id.btnSave)).perform(scrollTo(), click())
        onView(isRoot()).perform((pressBack()))
        Thread.sleep(WAIT_FOR_UI_RESPONSE)
        // Add a new slot
        onView(withId(R.id.floatingActionButton)).perform(click())
        onView(withText(StringResource.getResourceString(R.string.confirm)))
            .perform(click())
        // Save transaction
        onView(withId(R.id.ivSaveTransaction)).perform(click())
        Thread.sleep(WAIT_FOR_UI_RESPONSE)
        // Check slot duration
        onView(withId(R.id.slotList)).perform(
            RecyclerViewActions.actionOnItemAtPosition<BaseViewHolder<TimeSlotItem>>(
                FIRST_SLOT_POSITION,
                click()
            )
        )
        onView(withId(R.id.tvDurationDetail))
            .check(ViewAssertions.matches(ViewMatchers.withText("00:42")))

        onView(withText(StringResource.getResourceString(R.string.confirm)))
            .perform(click())
    }

    fun logoutRoutine() {
        // Logout
        onView(withId(R.id.config)).perform(click())
        onView(withId(R.id.btnLogout)).perform(click())
    }

    @After
    fun shutdownTest() {
        clearQueue()

        logoutRoutine()

        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        openActivityRule.scenario.close()
    }
}
