# MVC Structure and Flow in Coffee POS Application

This document explains the Model-View-Controller (MVC) architectural pattern as implemented in the Coffee POS JavaFX application. MVC is a software design pattern commonly used for developing user interfaces that divides the related program logic into three interconnected elements.

## 1. Model

The Model represents the core data and business logic of the application. It manages the data, logic, and rules of the application. In this Coffee POS system, the Model components are primarily found in the `coffee_pos.model` package.

**Key responsibilities of the Model:**
*   **Data Storage and Retrieval:** Interacting with the database (via `DBConnector`) to store and retrieve application data (e.g., products, orders, users, shifts, roles).
*   **Business Logic:** Implementing rules and operations related to the data (e.g., calculating total order amounts, managing product stock, user authentication logic).
*   **State Management:** Holding the current state of the application's data.

**Corresponding files/packages:**
*   `coffee_pos.model.*`:
    *   `Order.java`, `OrderItem.java`, `OrderReportItem.java`: Represent order-related data structures.
    *   `Product.java`: Represents product data.
    *   `Role.java`: Represents user roles.
    *   `SalesReportItem.java`: Data structure for sales reports.
    *   `Shift.java`: Represents staff shift data.
    *   `User.java`: Represents user data.
*   `coffee_pos.DBConnector.java`: Handles the database connection, which the models implicitly use to interact with the persistence layer.

## 2. View

The View is responsible for displaying the data from the Model to the user. It's the user interface (UI) of the application. In JavaFX, FXML files define the layout and visual elements of the views.

**Key responsibilities of the View:**
*   **Presentation:** Rendering the UI elements (buttons, text fields, tables, etc.).
*   **Displaying Data:** Taking data from the Model (often via the Controller) and presenting it in a user-friendly format.
*   **User Interaction (Passive):** Notifying the Controller about user input, but not handling the logic itself.

**Corresponding files/packages:**
*   `coffee_pos.view.*.fxml`:
    *   `InvoiceView.fxml`, `LoginView.fxml`, `MainView.fxml`, `OrderView.fxml`, `POSView.fxml`, `ProductView.fxml`, `RegisterView.fxml`, `ReportView.fxml`, `SettingsView.fxml`, `UserManagementView.fxml`: These FXML files define the visual layout and components of different screens in the application.

## 3. Controller

The Controller acts as an intermediary between the Model and the View. It receives user input from the View, processes it (often by interacting with the Model), and updates both the Model and the View accordingly.

**Key responsibilities of the Controller:**
*   **Handling User Input:** Responding to events triggered by user interactions (e.g., button clicks, text input).
*   **Updating the Model:** Translating user actions into operations on the Model (e.g., adding a product to an order, updating user information).
*   **Updating the View:** Retrieving data from the Model and instructing the View to display it.
*   **Application Logic:** Contains the logic for how the application responds to user actions and manages the flow between different views.

**Corresponding files/packages:**
*   `coffee_pos.controller.*.java`:
    *   `InvoiceController.java`, `LoginController.java`, `MainViewController.java`, `OrderController.java`, `POSController.java`, `ProductController.java`, `RegisterController.java`, `ReportController.java`, `SettingsController.java`, `UserManagementController.java`: Each of these Java classes manages the logic and interactions for a specific FXML view.

## Flow of Control and Data (MVC Interaction)

Here's a typical flow of interaction within the Coffee POS application following the MVC pattern:

1.  **User Interaction (View -> Controller):** The user interacts with a UI element in the **View** (e.g., clicks the "Add Product" button in `POSView.fxml`).
2.  **Event Handling (Controller):** The **Controller** (`POSController.java`) associated with that View receives the event (e.g., `handleAddProduct()`).
3.  **Business Logic/Data Manipulation (Controller -> Model):** The Controller then performs necessary business logic. This often involves:
    *   Retrieving data from the **Model** (e.g., fetching product details from the database using `Product` model and `DBConnector`).
    *   Updating the **Model** (e.g., creating a new `Order` or `OrderItem` object, persisting changes to the database).
4.  **Model Updates (Model):** The Model performs its operations (e.g., updates product stock in the database).
5.  **View Update (Controller -> View):** After the Model has been updated, the Controller retrieves the necessary updated data from the Model and instructs the **View** to refresh its display (e.g., updating the order table, refreshing the product grid).

This separation of concerns makes the application more modular, easier to maintain, and more scalable.
