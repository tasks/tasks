package org.tasks.billing;

import com.todoroo.astrid.billing.BillingConstants;
import com.todoroo.astrid.billing.BillingService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.tasks.TestUtilities.resetPreferences;

@Ignore("Throws mockito exception on Travis for some reason")
@RunWith(RobolectricTestRunner.class)
public class PurchaseHandlerTest {

    BillingService billingService;
    PurchaseHandler purchaseHandler;

    @Before
    public void before() {
        resetPreferences();
        billingService = mock(BillingService.class);
        purchaseHandler = new PurchaseHandler(billingService);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(billingService);
    }

    @Test
    public void userHasNotDonatedByDefault() {
        assertFalse(purchaseHandler.userDonated());
    }

    @Test
    public void billingNotSupportedByDefault() {
        assertFalse(purchaseHandler.isBillingSupported());
    }

    @Test
    public void haveNotRestoredTransactionsByDefault() {
        assertFalse(purchaseHandler.restoredTransactions());
    }

    @Test
    public void restoreTransactions() {
        purchaseHandler.onBillingSupported(true, BillingConstants.ITEM_TYPE_INAPP);

        verify(billingService).restoreTransactions();
    }

    @Test
    public void dontRestoreWhenBillingNotSupported() {
        purchaseHandler.onBillingSupported(false, BillingConstants.ITEM_TYPE_INAPP);
    }

    @Test
    public void dontRestoreWhenAlreadyRestored() {
        purchaseHandler.onRestoreTransactionsResponse(BillingConstants.ResponseCode.RESULT_OK);
        purchaseHandler.onBillingSupported(true, BillingConstants.ITEM_TYPE_INAPP);
    }

    @Test
    public void ignoreSubscriptions() {
        purchaseHandler.onBillingSupported(true, BillingConstants.ITEM_TYPE_SUBSCRIPTION);
    }

    @Test
    public void userDonated() {
        purchaseHandler.onPurchaseStateChange(BillingConstants.PurchaseState.PURCHASED, BillingConstants.TASKS_DONATION_ITEM_ID);

        assertTrue(purchaseHandler.userDonated());
    }

    @Test
    public void ignoreFailedTransaction() {
        purchaseHandler.onPurchaseStateChange(BillingConstants.PurchaseState.CANCELED, BillingConstants.TASKS_DONATION_ITEM_ID);

        assertFalse(purchaseHandler.userDonated());
    }

    @Test
    public void ignoreOldItems() {
        purchaseHandler.onPurchaseStateChange(BillingConstants.PurchaseState.PURCHASED, "some old purchase");

        assertFalse(purchaseHandler.userDonated());
    }

    @Test
    public void oldItemsDontReplaceLatest() {
        purchaseHandler.onPurchaseStateChange(BillingConstants.PurchaseState.PURCHASED, BillingConstants.TASKS_DONATION_ITEM_ID);
        purchaseHandler.onPurchaseStateChange(BillingConstants.PurchaseState.PURCHASED, "some old purchase");

        assertTrue(purchaseHandler.userDonated());
    }

    @Test
    public void restoredTransactions() {
        purchaseHandler.onRestoreTransactionsResponse(BillingConstants.ResponseCode.RESULT_OK);

        assertTrue(purchaseHandler.restoredTransactions());
    }

    @Test
    public void restoreTransactionsFailed() {
        purchaseHandler.onRestoreTransactionsResponse(BillingConstants.ResponseCode.RESULT_DEVELOPER_ERROR);

        assertFalse(purchaseHandler.restoredTransactions());
    }
}
