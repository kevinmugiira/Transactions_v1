package org.kevin.events;


/** Topics that other services are allowed to consume. */
public final class Topics {

    /** Outcomes of money movements. Key = account id, so one account's events stay in order. */
    public static final String TRANSACTION_EVENTS = "transaction.events";

    private Topics() { }
}
