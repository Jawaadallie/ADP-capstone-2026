package Capstone.ui;

import Capstone.app.Session;
import Capstone.model.Product;
import Capstone.model.Review;
import Capstone.net.Api;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ReviewPanel extends JPanel {

    // Stores the actual Product objects retrieved from the backend.
    // The position of each Product matches its name in productNames.
    private final List<Product> products = new ArrayList<Product>();

    // Stores the product names displayed in the JList.
    private final DefaultListModel<String> productNames =
            new DefaultListModel<String>();

    // Displays the list of available products.
    // It uses productNames as its data model.
    private final JList<String> productList =
            new JList<String>(productNames);

    // Displays the name of the currently selected product.
    private final JLabel productTitle = new JLabel("Choose a product");

    // Displays the average rating and number of reviews.
    private final JLabel summary = new JLabel(" ");

    // Container that holds all the individual product reviews.
    private final JPanel reviewsBox = new JPanel();

    // Allows the customer to select a rating between 1 and 5.
    private final JComboBox<Integer> rating =
            new JComboBox<Integer>(new Integer[]{5, 4, 3, 2, 1});

    // Text area where the customer writes their review.
    private final JTextArea comment = new JTextArea(3, 20);

    // Displays messages such as validation errors or success messages.
    private final JLabel message = new JLabel(" ");

    // Button that submits the customer's review.
    private final JButton submit = new JButton("Submit review");



    public ReviewPanel() {

        // Uses BorderLayout to divide the screen into regions.
        // 16 represents the horizontal gap between components.
        setLayout(new BorderLayout(16, 0));

        // Sets the background colour of the entire panel.
        setBackground(new Color(0xF3F6F8));

        // Changes the product title font.
        productTitle.setFont(new Font(" UI", Font.BOLD, 22));

        // Adds the product selection panel to the WEST.
        add(createProductPanel(), BorderLayout.WEST);

        // Adds the review details and submission panel to the CENTER.
        add(createDetailPanel(), BorderLayout.CENTER);


        // ListSelectionListener detects when a customer selects
        // a different product from the JList.
        productList.addListSelectionListener(
                new ListSelectionListener() {

                    @Override
                    public void valueChanged(ListSelectionEvent e) {

                        // Prevents the method from running multiple times
                        // while the selection is still being adjusted.
                        if (!e.getValueIsAdjusting()) {

                            // Loads reviews belonging to the selected product.
                            loadReviews();
                        }
                    }
                });


        // ActionListener detects when the submit button is clicked.
        submit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                // Calls the method responsible for submitting a review.
                submitReview();
            }
        });

        // Retrieves the available products from the backend
        // when the ReviewPanel is first created.
        loadProducts();
    }


    private JPanel createProductPanel() {

        // Creates a panel using BorderLayout.
        JPanel panel = new JPanel(new BorderLayout());

        // Sets the preferred width of the product panel to 300 pixels.
        panel.setPreferredSize(new Dimension(300, 10));

        // Adds a heading at the top of the panel.
        panel.add(new JLabel("Products"), BorderLayout.NORTH);

        // Allows only one product to be selected at a time.
        productList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        // JScrollPane adds scrolling functionality to the product list.
        panel.add(new JScrollPane(productList), BorderLayout.CENTER);

        return panel;
    }


    private JPanel createDetailPanel() {

        JPanel panel = new JPanel(new BorderLayout());

        // GridLayout arranges the product title and summary vertically.
        JPanel head = new JPanel(new GridLayout(0, 1));

        // Adds the selected product name and its rating summary.
        head.add(productTitle);
        head.add(summary);

        // Places the heading at the top of the detail panel.
        panel.add(head, BorderLayout.NORTH);


        // BoxLayout arranges reviews vertically, one below another.
        reviewsBox.setLayout(
                new BoxLayout(reviewsBox, BoxLayout.Y_AXIS));

        // Wrapper holds the reviews inside a BorderLayout.
        JPanel wrapper = new JPanel(new BorderLayout());

        // Places reviews at the top of the wrapper.
        wrapper.add(reviewsBox, BorderLayout.NORTH);

        // Adds scrolling functionality to the reviews section.
        JScrollPane scroll = new JScrollPane(wrapper);

        // Prevents horizontal scrolling.
        scroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Places the reviews section in the centre.
        panel.add(scroll, BorderLayout.CENTER);

        // Places the review submission form at the bottom.
        panel.add(createForm(), BorderLayout.SOUTH);

        return panel;
    }


    private JPanel createForm() {

        JPanel form = new JPanel(new BorderLayout());

        // Adds the form heading.
        form.add(new JLabel("Write a review"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new BorderLayout());


        // Creates a row containing the rating label and dropdown.
        JPanel ratingRow = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        ratingRow.add(new JLabel("Rating"));
        ratingRow.add(rating);

        // Places the rating dropdown at the top.
        fields.add(ratingRow, BorderLayout.NORTH);


        // Allows long comments to continue on the next line.
        comment.setLineWrap(true);

        // Makes wrapping happen between words instead of characters.
        comment.setWrapStyleWord(true);

        // Adds scrolling to the comment area.
        fields.add(new JScrollPane(comment), BorderLayout.CENTER);

        // Adds the input fields to the form.
        form.add(fields, BorderLayout.CENTER);


        // Creates the bottom section containing the message and button.
        JPanel south = new JPanel(new BorderLayout());

        // Displays success or error messages.
        south.add(message, BorderLayout.CENTER);

        // Places the submit button on the right.
        south.add(submit, BorderLayout.EAST);

        form.add(south, BorderLayout.SOUTH);

        return form;
    }


    private void loadProducts() {

        // SwingWorker allows us to perform HTTP requests
        // without freezing the Swing user interface.
        //
        // List<Product> is the data returned by the background task.
        // Void means we are not publishing intermediate results.
        SwingWorker<List<Product>, Void> worker =
                new SwingWorker<List<Product>, Void>() {

                    // doInBackground() executes on a separate background thread.
                    // This is where we perform the HTTP GET request.
                    @Override
                    protected List<Product> doInBackground() throws Exception {

                        // Sends GET /products to the backend.
                        //
                        // TypeReference tells Jackson that the JSON response
                        // should be converted into a List of Product objects.
                        return Api.get(
                                "/products",
                                new TypeReference<List<Product>>() {}
                        );
                    }


                    // done() executes on the Swing Event Dispatch Thread.
                    // This is where we safely update Swing components.
                    @Override
                    protected void done() {

                        try {

                            // get() retrieves the result returned by doInBackground().
                            // It also allows us to catch exceptions from the request.
                            List<Product> loaded = get();

                            // Clears previously loaded products.
                            products.clear();

                            // Clears previously displayed product names.
                            productNames.clear();

                            // Checks whether the backend returned products.
                            if (loaded != null) {

                                // Loops through every Product returned by the API.
                                for (Product product : loaded) {

                                    // Stores the actual Product object.
                                    products.add(product);

                                    // Adds its name to the visible JList.
                                    productNames.addElement(
                                            product.getProductName());
                                }
                            }

                            // If products exist, automatically select the first one.
                            // This triggers the selection listener and loads its reviews.
                            if (productNames.size() > 0) {
                                productList.setSelectedIndex(0);
                            }

                        } catch (Exception e) {

                            // Displays an error if retrieving products fails.
                            summary.setText(
                                    "Could not load products: " + e.getMessage());
                        }
                    }
                };

        // Starts the SwingWorker.
        worker.execute();
    }


    private void loadReviews() {

        // Retrieves the index of the selected product in the JList.
        int index = productList.getSelectedIndex();

        // -1 means that no product has been selected.
        if (index == -1) {
            return;
        }

        // Retrieves the actual Product object using the selected index.
        // This works because products and productNames use the same order.
        final Product product = products.get(index);

        // Updates the heading to show the selected product.
        productTitle.setText(product.getProductName());

        // Displays a loading message while the request is processing.
        summary.setText("Loading reviews...");


        // Creates a background task to retrieve product reviews.
        SwingWorker<List<Review>, Void> worker =
                new SwingWorker<List<Review>, Void>() {

                    @Override
                    protected List<Review> doInBackground() throws Exception {

                        // Sends a GET request to:
                        // /reviews/product/{productId}
                        //
                        // The product ID determines which reviews are retrieved.
                        return Api.get(
                                "/reviews/product/" + product.getProductId(),
                                new TypeReference<List<Review>>() {}
                        );
                    }


                    @Override
                    protected void done() {

                        try {

                            // Gets the reviews returned by the backend.
                            List<Review> reviews = get();

                            // Prevents a NullPointerException if the API returns null.
                            // Instead, we work with an empty list.
                            if (reviews == null) {
                                reviews = new ArrayList<Review>();
                            }

                            // Passes the reviews to the method that displays them.
                            showReviews(reviews);

                        } catch (Exception e) {

                            // Displays an error if the reviews cannot be retrieved.
                            summary.setText(
                                    "Could not load reviews: " + e.getMessage());
                        }
                    }
                };

        // Executes the background request.
        worker.execute();
    }


    private void showReviews(List<Review> reviews) {

        // Removes previously displayed reviews.
        // This prevents reviews from different products mixing together.
        reviewsBox.removeAll();

        // Checks whether the selected product has any reviews.
        if (reviews.size() == 0) {

            // Displays a message when no reviews exist.
            summary.setText("No reviews yet");

            reviewsBox.add(
                    new JLabel(
                            "Nobody has reviewed this product yet. Be the first!"));

        } else {

            // Variable used to calculate the total rating.
            double sum = 0;

            // Loops through every review.
            for (Review review : reviews) {

                // Adds the rating of the current review to the total.
                sum = sum + review.getRating();
            }

            // Calculates the average rating.
            // Example: 15 total stars / 3 reviews = 5.0.
            double average = sum / reviews.size();


            // Converts the average to an integer for displaying stars.
            // For example, 4.7 becomes 4 stars.
            //
            // String.format("%.1f", average) displays one decimal place.
            summary.setText(
                    makeStars((int) average) + "  "
                            + String.format("%.1f", average)
                            + " out of 5  ("
                            + reviews.size() + " reviews)"
            );


            // Displays each individual review.
            for (Review review : reviews) {

                // Creates a panel for the review and adds it to reviewsBox.
                reviewsBox.add(createReviewItem(review));
            }
        }

        // Tells Swing that the layout needs to be recalculated.
        reviewsBox.revalidate();

        // Repaints the panel so the updated reviews become visible.
        reviewsBox.repaint();
    }


    private JPanel createReviewItem(Review review) {

        // Creates a panel with BorderLayout.
        JPanel item = new JPanel(new BorderLayout());

        // Adds a light grey border around the review.
        item.setBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // Prevents the review panel from becoming excessively tall.
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));


        // Displays the rating as stars at the top.
        item.add(
                new JLabel(makeStars(review.getRating())),
                BorderLayout.NORTH
        );


        // Creates a text area containing the customer's comment.
        JTextArea text = new JTextArea(review.getComment());

        // Prevents customers from editing reviews belonging to others.
        text.setEditable(false);

        // Allows long comments to wrap onto multiple lines.
        text.setLineWrap(true);
        text.setWrapStyleWord(true);

        // Places the comment in the centre of the review panel.
        item.add(text, BorderLayout.CENTER);

        return item;
    }


    private void submitReview() {

        // Gets the currently selected product index.
        int index = productList.getSelectedIndex();

        // A customer must select a product before submitting a review.
        if (index == -1) {

            message.setForeground(Color.RED);
            message.setText("Please choose a product first.");

            // Stops execution if no product was selected.
            return;
        }

        // Retrieves the actual selected Product object.
        final Product product = products.get(index);


        // Gets the comment and removes unnecessary spaces
        // at the beginning and end.
        final String text = comment.getText().trim();

        // Validates that the customer entered a comment.
        if (text.isEmpty()) {

            message.setForeground(Color.RED);
            message.setText("Please write a short comment.");

            return;
        }


        // Gets the rating selected from the JComboBox.
        // The selected Object is converted into an Integer.
        final int stars = (Integer) rating.getSelectedItem();


        // Clears the previous message.
        message.setText(" ");

        // Disables the submit button to prevent multiple submissions
        // while the HTTP request is still processing.
        submit.setEnabled(false);


        // Creates a background task for submitting the review.
        SwingWorker<Void, Void> worker =
                new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() throws Exception {

                        // Creates a Map representing the customer object.
                        // The backend expects a customer reference containing
                        // the customer's ID.
                        Map<String, Object> customer = new HashMap<>();

                        // Retrieves the currently logged-in customer's ID.
                        customer.put("customerId", Session.customerId());


                        // Creates a Map representing the product object.
                        Map<String, Object> productMap = new HashMap<>();

                        // Adds the selected product's ID.
                        productMap.put("productId", product.getProductId());


                        // Creates the main request body.
                        // This will eventually be converted into JSON by Api.send().
                        Map<String, Object> body = new HashMap<>();

                        // Associates the review with the customer.
                        body.put("customer", customer);

                        // Associates the review with the selected product.
                        body.put("product", productMap);

                        // Adds the customer's rating.
                        body.put("rating", stars);

                        // Adds the customer's written comment.
                        body.put("comment", text);


                        // Sends a POST request to /reviews.
                        // The backend receives the JSON and creates the review.
                        Api.send("POST", "/reviews", body);

                        // Void means that this background task does not return
                        // a meaningful result.
                        return null;
                    }


                    @Override
                    protected void done() {

                        // Re-enables the submit button once the request finishes.
                        submit.setEnabled(true);

                        try {

                            // Checks whether doInBackground() completed successfully.
                            // If an exception occurred, get() throws it here.
                            get();

                            // Clears the comment field after successful submission.
                            comment.setText("");

                            // Displays the success message in green.
                            message.setForeground(Color.GREEN.darker());
                            message.setText("Thanks for your review!");

                            // Reloads the reviews so the newly submitted review
                            // appears immediately on the screen.
                            loadReviews();

                        } catch (Exception e) {

                            // Displays the error returned by the failed request.
                            message.setForeground(Color.RED);
                            message.setText(e.getMessage());
                        }
                    }
                };

        // Starts the review submission background task.
        worker.execute();
    }



    private String makeStars(int count) {

        // Stores the final string of stars.
        String result = "";

        // Always creates exactly five-star symbols.
        for (int i = 1; i <= 5; i++) {

            // If the current position is within the rating,
            // display a filled star.
            if (i <= count) {

                result = result + "*";

            } else {

                // Otherwise, display an empty star.
                result = result + "*";
            }
        }

        // Returns the completed five-star string.
        return result;
    }
}