package Capstone.app;

import Capstone.model.Customer;
import Capstone.net.Api;

public final class Session {

    private static Customer customer;

    private Session() {}

    public static void signIn(Customer c) {
        customer = c;
        Cart.get().clear();
    }

    public static void signOut() {
        customer = null;
        Api.clearCredentials();
        Cart.get().clear();
    }

    public static Customer customer() {
        return customer;
    }

    public static String customerId() {
        return customer == null ? null : customer.getCustomerId();
    }

    public static String role() {
        if ( customer  == null || customer.getRole() == null) {
            return "CUSTOMER";
        }
        return customer.getRole();
    }

    public static boolean isStaff() {
        return role().equals("STAFF") || role().equals("OWNER");
    }

    public static boolean isOwner(){
        return role().equals("OWNER");
    }
}
