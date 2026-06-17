package com.kendi.pos;

import com.kendi.pos.category.Category;
import com.kendi.pos.category.CategoryRepository;
import com.kendi.pos.product.Product;
import com.kendi.pos.product.ProductRepository;
import com.kendi.pos.restotable.RestoTable;
import com.kendi.pos.restotable.RestoTableRepository;
import com.kendi.pos.staff.Staff;
import com.kendi.pos.staff.StaffRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class PosApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosApplication.class, args);
	}

	@Bean
	public CommandLineRunner seedData(
			StaffRepository staffRepo,
			CategoryRepository categoryRepo,
			ProductRepository productRepo,
			RestoTableRepository tableRepo
	) {
		return args -> {
			long now = System.currentTimeMillis();

			// ─── STAFF ───
			if (staffRepo.count() == 0) {
				Staff admin = new Staff();
				admin.setName("Admin");
				admin.setPinHash(BCrypt.hashpw("0000", BCrypt.gensalt()));
				admin.setRole("admin");
				admin.setActive(true);
				admin.setCreatedAt(now);
				staffRepo.save(admin);

				Staff banakier = new Staff();
				banakier.setName("Banakier");
				banakier.setPinHash(BCrypt.hashpw("1234", BCrypt.gensalt()));
				banakier.setRole("cashier");
				banakier.setActive(true);
				banakier.setCreatedAt(now);
				staffRepo.save(banakier);

				System.out.println("✅ Staff seeded");
			}

			// ─── CATEGORIES ───
			Map<String, String> catIds = new HashMap<>();
			if (categoryRepo.count() == 0) {
				String[][] cats = {
						{"Kafe", "#F59E0B", "1"},
						{"Pijet", "#06B6D4", "2"},
						{"Alkohol", "#8B5CF6", "3"},
						{"Cocktails", "#3B82F6", "4"},
						{"Sallata", "#10B981", "5"},
						{"Pasta", "#84CC16", "6"},
						{"Pizza", "#F97316", "7"},
						{"Embelsira", "#EC4899", "8"}
				};
				for (String[] c : cats) {
					Category cat = new Category();
					cat.setName(c[0]);
					cat.setColor(c[1]);
					cat.setSortOrder(Integer.parseInt(c[2]));
					cat.setCreatedAt(now);
					cat = categoryRepo.save(cat);
					catIds.put(c[0], cat.getId());
				}
				System.out.println("✅ Categories seeded");
			}

			// ─── PRODUCTS ───
			if (productRepo.count() == 0 && !catIds.isEmpty()) {
				Object[][] prods = {
						{"Kafe", "Espresso", 120},
						{"Kafe", "Espresso Doppio", 200},
						{"Kafe", "Macchiato", 150},
						{"Kafe", "Cappuccino", 200},
						{"Kafe", "Latte Macchiato", 220},
						{"Kafe", "Americano", 180},
						{"Kafe", "Turke", 150},

						{"Pijet", "Coca Cola", 200},
						{"Pijet", "Fanta", 200},
						{"Pijet", "Sprite", 200},
						{"Pijet", "Ujë", 100},
						{"Pijet", "Lëng portokalli", 250},

						{"Alkohol", "Birrë Peja", 250},
						{"Alkohol", "Birrë Heineken", 300},
						{"Alkohol", "Verë e kuqe", 400},
						{"Alkohol", "Raki", 200},

						{"Cocktails", "Mojito", 500},
						{"Cocktails", "Margarita", 550},
						{"Cocktails", "Aperol Spritz", 500},

						{"Sallata", "Sallatë Cezar", 600},
						{"Sallata", "Sallatë Greke", 550},

						{"Pasta", "Spaghetti Carbonara", 750},
						{"Pasta", "Penne Arrabiata", 700},

						{"Pizza", "Pizza Margherita", 700},
						{"Pizza", "Pizza Capricciosa", 850},
						{"Pizza", "Pizza Quattro Stagioni", 900},

						{"Embelsira", "Tiramisu", 450},
						{"Embelsira", "Crème Brûlée", 450}
				};
				int order = 1;
				for (Object[] p : prods) {
					Product prod = new Product();
					prod.setName((String) p[1]);
					prod.setCategoryId(catIds.get((String) p[0]));
					prod.setPrice((Integer) p[2]);
					prod.setSortOrder(order++);
					prod.setActive(true);
					prod.setCreatedAt(now);
					productRepo.save(prod);
				}
				System.out.println("✅ Products seeded");
			}

			// ─── TABLES ───
			if (tableRepo.count() == 0) {
				for (int i = 1; i <= 10; i++) {
					RestoTable t = new RestoTable();
					t.setName("T" + i);
					t.setSortOrder(i);
					t.setCreatedAt(now);
					tableRepo.save(t);
				}
				System.out.println("✅ Tables seeded");
			}

			System.out.println("🚀 Seed kompletë!");
		};
	}
}