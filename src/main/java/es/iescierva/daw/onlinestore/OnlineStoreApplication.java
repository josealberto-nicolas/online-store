package es.iescierva.daw.onlinestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

// --- MODELOS DE DATOS (Java 17 Records y Clases) ---

/**
 * Representa un producto tecnológico en la tienda.
 * Usamos un record de Java 17 para un modelo de datos inmutable y conciso.
 */
record Product(Long id, String name, String description, BigDecimal price, String imageUrl) {}

/**
 * Representa un artículo específico dentro del carrito de compras.
 */
class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return product.price().multiply(new BigDecimal(quantity));
    }
}

/**
 * Representa una orden de compra finalizada.
 */
record Order(Long id, Map<Long, CartItem> items, BigDecimal totalAmount, String status, Date orderDate) {}

// --- SERVICIOS EN MEMORIA ---

/**
 * Servicio para gestionar el catálogo de productos.
 * Simula la base de datos de productos.
 */
@Service
class ProductService {
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong productIdCounter = new AtomicLong(0);

    // Inicializa el catálogo de productos tecnológicos
    public ProductService() {
        // Productos actualizados con imágenes reales
        addProduct("Portátil Ultraligero X", "Potente portátil con pantalla OLED de 14 pulgadas.", new BigDecimal("1299.99"), "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=600&q=80");
        addProduct("Smartphone Z Pro", "El último modelo con cámara de 200MP y acabado titanio.", new BigDecimal("999.50"), "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80");
        addProduct("Auriculares NoiseCancel", "Cancelación de ruido activa y sonido de alta fidelidad.", new BigDecimal("199.90"), "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80");
        addProduct("Smartwatch Series 7", "Monitoreo de salud completo y correa deportiva.", new BigDecimal("299.00"), "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80");
        
        // Nuevos productos añadidos
        addProduct("Tablet Pro 12.9", "Pantalla Liquid Retina ideal para creativos.", new BigDecimal("1099.00"), "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=600&q=80");
        addProduct("Cámara Mirrorless 4K", "Sensor Full Frame para fotografía profesional.", new BigDecimal("2100.00"), "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=600&q=80");
        addProduct("Teclado Mecánico RGB", "Switches azules y retroiluminación personalizable.", new BigDecimal("89.99"), "https://images.unsplash.com/photo-1587829741301-dc798b91add1?auto=format&fit=crop&w=600&q=80");
        addProduct("Monitor UltraWide 34\"", "Curvo para una experiencia inmersiva en gaming.", new BigDecimal("459.50"), "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80");
        addProduct("Consola Retro Game", "Diseño clásico con juegos modernos preinstalados.", new BigDecimal("129.99"), "https://images.unsplash.com/photo-1486401899868-0e435ed85128?auto=format&fit=crop&w=600&q=80");
        addProduct("Altavoz Inteligente Home", "Asistente de voz integrado y sonido 360º.", new BigDecimal("79.99"), "https://images.unsplash.com/photo-1543512214-318c77a072bf?auto=format&fit=crop&w=600&q=80");
    }

    private void addProduct(String name, String description, BigDecimal price, String imageUrl) {
        Long id = productIdCounter.incrementAndGet();
        products.put(id, new Product(id, name, description, price, imageUrl));
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }
}

/**
 * Servicio para gestionar el carrito de compras (sesión).
 */
@Service
@SessionAttributes("cart")
class CartService {
    private final ProductService productService;
    private final Map<Long, CartItem> cart = new ConcurrentHashMap<>();

    // Contador simple para simular IDs de órdenes
    private final AtomicLong orderIdCounter = new AtomicLong(1000);

    public CartService(ProductService productService) {
        this.productService = productService;
    }

    @ModelAttribute("cart")
    public Map<Long, CartItem> getCart() {
        return cart;
    }

    public void addItem(Long productId, int quantity) {
        productService.findById(productId).ifPresent(product -> {
            cart.compute(productId, (id, item) -> {
                if (item == null) {
                    return new CartItem(product, quantity);
                } else {
                    item.setQuantity(item.getQuantity() + quantity);
                    return item;
                }
            });
        });
    }

    public void updateItemQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            cart.remove(productId);
        } else {
            cart.computeIfPresent(productId, (id, item) -> {
                item.setQuantity(quantity);
                return item;
            });
        }
    }

    public BigDecimal getCartTotal() {
        return cart.values().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void clearCart() {
        cart.clear();
    }

    public Order checkout() {
        if (cart.isEmpty()) {
            throw new IllegalStateException("El carrito está vacío. No se puede proceder con el pago.");
        }

        // Simulación de proceso de pago
        // En una aplicación real, aquí se llamaría a una API de pago (Stripe, PayPal, etc.)

        BigDecimal total = getCartTotal();
        Map<Long, CartItem> finalItems = new HashMap<>(cart); // Clonamos los ítems antes de vaciar el carrito

        // Generar la orden
        Order newOrder = new Order(
            orderIdCounter.incrementAndGet(),
            finalItems,
            total,
            "PAGADO", // Estado fijo para la simulación
            new Date()
        );

        clearCart(); // Vaciar el carrito después de la compra exitosa
        return newOrder;
    }
}

// --- CONTROLADOR WEB (SPRING MVC) ---

@Controller
class StoreController {
    private final ProductService productService;
    private final CartService cartService;

    public StoreController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    /**
     * Muestra la página principal con el catálogo de productos y el carrito.
     */
    @GetMapping("/")
    public String viewStore(Model model) {
        // Cargar todos los productos para el catálogo
        model.addAttribute("products", productService.findAll());
        
        // El carrito (Map<Long, CartItem>) se inyecta automáticamente desde @SessionAttributes
        Map<Long, CartItem> cart = cartService.getCart();
        model.addAttribute("cartItems", cart.values());
        model.addAttribute("cartTotal", cartService.getCartTotal());

        return "store"; // Nombre de la plantilla Thymeleaf: store.html
    }

    /**
     * Maneja la adición de un producto al carrito.
     */
    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes) {
        
        if (quantity <= 0) {
            redirectAttributes.addFlashAttribute("error", "La cantidad debe ser mayor que cero.");
            return "redirect:/";
        }

        cartService.addItem(productId, quantity);
        redirectAttributes.addFlashAttribute("success", "Producto añadido al carrito con éxito.");
        return "redirect:/";
    }

    /**
     * Maneja la actualización de la cantidad de un artículo en el carrito.
     */
    @PostMapping("/update")
    public String updateCart(@RequestParam("productId") Long productId,
                             @RequestParam("quantity") int quantity,
                             RedirectAttributes redirectAttributes) {

        if (quantity < 0) {
            redirectAttributes.addFlashAttribute("error", "La cantidad no puede ser negativa.");
            return "redirect:/";
        }
        
        cartService.updateItemQuantity(productId, quantity);

        if (quantity == 0) {
            redirectAttributes.addFlashAttribute("success", "Producto eliminado del carrito.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Cantidad actualizada.");
        }
        return "redirect:/";
    }

    /**
     * Simula el proceso de pago.
     */
    @PostMapping("/checkout")
    public String checkout(RedirectAttributes redirectAttributes) {
        try {
            Order order = cartService.checkout();
            // La orden se almacena en FlashAttributes para mostrar los detalles en la página de éxito.
            redirectAttributes.addFlashAttribute("order", order);
            redirectAttributes.addFlashAttribute("checkout_success", "¡Pago realizado con éxito! Gracias por tu compra.");
            return "redirect:/success";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Muestra la página de éxito después del pago.
     */
    @GetMapping("/success")
    public String orderSuccess(@ModelAttribute("order") Order order, Model model) {
        if (order == null || order.id() == null) {
             // Si el objeto 'order' no está presente (por ejemplo, acceso directo sin pasar por checkout)
            return "redirect:/";
        }
        model.addAttribute("order", order);

        // --- CORRECCIÓN: Recargar datos necesarios para la vista 'store.html' ---
        // La vista siempre necesita 'products', 'cartItems' y 'cartTotal' para renderizarse sin errores.
        model.addAttribute("products", productService.findAll());
        
        Map<Long, CartItem> cart = cartService.getCart();
        model.addAttribute("cartItems", cart.values());
        model.addAttribute("cartTotal", cartService.getCartTotal());
        // -----------------------------------------------------------------------

        return "store"; // Reutilizamos la plantilla y mostramos la confirmación en la parte superior.
    }
}

// --- CLASE PRINCIPAL DE SPRING BOOT ---

@SpringBootApplication
public class OnlineStoreApplication {
    public static void main(String[] args) {
        System.out.println("Iniciando la aplicación web de la tienda online...");
        SpringApplication.run(OnlineStoreApplication.class, args);
        System.out.println("La aplicación está lista. Accede a http://localhost:8080");
    }
}