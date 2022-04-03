package space.gavinklfong.demo.streamapi;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.gavinklfong.demo.streamapi.models.Customer;
import space.gavinklfong.demo.streamapi.models.Order;
import space.gavinklfong.demo.streamapi.models.Product;
import space.gavinklfong.demo.streamapi.repos.CustomerRepo;
import space.gavinklfong.demo.streamapi.repos.OrderRepo;
import space.gavinklfong.demo.streamapi.repos.ProductRepo;

@Slf4j
@Component
public class AppCommandRunner implements CommandLineRunner {

	@Autowired
	private CustomerRepo customerRepos;
	
	@Autowired
	private OrderRepo orderRepos;
	
	@Autowired
	private ProductRepo productRepos;

	
	@Override
	public void run(String... args) throws Exception {
		log.info("running runner");
		List<Customer> customers = customerRepos.findAll();
		customers.forEach(System.out::println);

//MY SOLUTIONS
//		List<Product> bookProducts=productRepos.findAlL().filter(product->product.getCategory.equalsIgnoreCase("books")).filter(product->product.getPrice()>100).Collect(Collecteors.toList())

//	List<Order> orders=orderRepo.findALl().stream().filter(order->order.getProducts().stream().
//										anyMatch(product->product.getName().equalsIgnoreCase("baby"))
//										.collectc(Collectors.toList())

//		Obtain a list of product with category = “Toys” and then apply 10% discount
//
//			List<Products> toys=productRepo.findAll().stream().
//					filter(product->product.getCategory().equalsIgnoreCase("Toys")).
//					map(toys->toy.setPrice(toy.getPrice()*0.9))
//					.collect(Collectors.toList())




//		Exercise 4 — Obtain a list of products ordered by customer of tier 2 between 01-Feb-2021 and 01-Apr-2021

//		Set<Products> productList= OrderRepo.findAll().stream().filter(order -> order.getCustomer().equalsIgnoreCase("Tier2")).filter(order -> 01 - Feb - 2021 < order.getOrderDate() < 01 - Apr - 2021)
//													.map(order->order.getProducts()).collect(Collectors.toSet())
//
//		List<Product> result = orderRepo.findAll()
//				.stream()
//				.filter(o -> o.getCustomer().getTier() == 2)
//				.filter(o -> o.getOrderDate().compareTo(LocalDate.of(2021, 2, 1)) >= 0)
//				.filter(o -> o.getOrderDate().compareTo(LocalDate.of(2021, 4, 1)) <= 0)
//				.flatMap(o -> o.getProducts().stream())
//				.distinct()
//				.collect(Collectors.toList());

		List<Order> orders = orderRepos.findAll();
				orders.strem().filter(product->product.get)
		
//		orders.stream()
//		.map(o -> o.getCustomer())
//		.forEach(i -> System.out.println(i.getName()));
		
//		log.info("Customer count: " + count);

//		orders.stream()
//		.flatMap(o -> o.getProducts().stream())
//		.forEach(System.out::println);
		
		
		List<Product> products = productRepos.findAll();
		products.stream()
		.flatMap(p -> p.getOrders().stream())
		.forEach(System.out::println);
	}

}
