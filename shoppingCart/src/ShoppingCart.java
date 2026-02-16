import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

record Money(BigDecimal amount) {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money (double amount) {
        this(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(double factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    @Override
    public String toString() {
        return "$" + amount.toString();
    }
}

class Product {
    private final String id;
    private final String name;
    private final boolean ageRestricted;

    public Product(String id, String name, boolean ageRestricted) {
        this.id = id;
        this.name = name;
        this.ageRestricted = ageRestricted;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAgeRestricted() {
        return ageRestricted;
    }
}

class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public void addQuantity(int qty) {
        this.quantity += qty;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}

class Cart {
    private final Map<String, CartItem> items = new LinkedHashMap<>();

    public void add(Product product, int quantity) {
        items.compute(product.getId(), (id, existing) -> {
            if (existing == null) return new CartItem(product, quantity);
            existing.addQuantity(quantity);
            return existing;
        });
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableCollection(items.values()).stream().toList();
    }
}

interface PricingPolicy {
    Money getPrice(Product product);
}

class StandardPricingPolicy implements PricingPolicy {
    private final Map<String, Money> catalog = new HashMap<>();

    public void setPrice(Product product, Money price) {
        catalog.put(product.getId(), price);
    }

    @Override
    public Money getPrice(Product product) {
        return catalog.getOrDefault(product.getId(), Money.ZERO);
    }
}

class HappyHourPricingPolicy implements PricingPolicy {
    private final PricingPolicy pricingPolicy;

    public HappyHourPricingPolicy(PricingPolicy pricingPolicy) {
        this.pricingPolicy = pricingPolicy;
    }

    @Override
    public Money getPrice(Product product) {
        Money basePrice = pricingPolicy.getPrice(product);
        return basePrice.multiply(0.8); // 20% discount
    }
}

record Customer(int age) {}

record ReceiptLine(String productName, int quantity, Money unitPrice, Money totalPrice) {}

class Receipt {
    private final List<ReceiptLine> lines = new ArrayList<>();
    private Money total = Money.ZERO;

    public void addLine(ReceiptLine line) {
        lines.add(line);
        total = total.add(line.totalPrice());
    }

    public void print() {
        for (ReceiptLine line : lines) {
            System.out.println(line.quantity() + " x " + line.productName() + " @ " + line.unitPrice() + " = " + line.totalPrice());
        }
        System.out.println("Total: " + total);
    }
}

class CheckoutService {
    private final PricingPolicy pricingPolicy;

    public CheckoutService(PricingPolicy pricingPolicy) {
        this.pricingPolicy = pricingPolicy;
    }

    public Receipt checkout(Cart cart, Customer customer) {
        validateCart(cart, customer);
        Receipt receipt = new Receipt();

        for (CartItem item : cart.getItems()) {
            Money unitPrice = pricingPolicy.getPrice(item.getProduct());
            Money totalPrice = unitPrice.multiply(item.getQuantity());

            receipt.addLine(new ReceiptLine(
                    item.getProduct().getName(),
                    item.getQuantity(),
                    unitPrice,
                    totalPrice
            ));
        }

        return receipt;
    }

    private void validateCart(Cart cart, Customer customer) {
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().isAgeRestricted() && customer.age() < 18) {
                throw new IllegalStateException("Customer is not old enough to purchase age-restricted product: " + item.getProduct().getName());
            }
        }
    }
}
public class ShoppingCart {
    public static void main(String[] args) {
        Product beer = new Product("1", "Beer", true);
        Product bread = new Product("2", "Bread", false);

        StandardPricingPolicy standardPricing = new StandardPricingPolicy();
        standardPricing.setPrice(beer, new Money(5.00));
        standardPricing.setPrice(bread, new Money(2.00));

        Cart cart = new Cart();
        cart.add(beer, 2);
        cart.add(bread, 1);

        CheckoutService checkoutService = new CheckoutService(standardPricing);

        try {
            System.out.println("Attempting checkout for 16 year old:");
            Customer kid = new Customer(16);
            checkoutService.checkout(cart, kid);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Checkout for 21 year old:");
        Customer adult = new Customer(21);
        Receipt receipt = checkoutService.checkout(cart, adult);
        receipt.print();

        CheckoutService happyHourCheckout = new CheckoutService(new HappyHourPricingPolicy(standardPricing));
        Receipt happyHourReceipt = happyHourCheckout.checkout(cart, adult);
        happyHourReceipt.print();
    }
}