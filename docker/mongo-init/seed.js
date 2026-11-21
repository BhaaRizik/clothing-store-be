db = db.getSiblingDB('clothing_store');

if (db.categories.countDocuments({}) > 0) {
  print('[seed] categories already present - skipping seed (database is not empty).');
  quit(0);
}

const now = new Date();
const daysAgo = (d) => new Date(now.getTime() - d * 24 * 60 * 60 * 1000);
const money = (v) => NumberDecimal(v);
const img = (slug, alt) => [{ url: `/images/catalog/${slug}.svg`, alt: alt }];

const cat = {
  men: ObjectId(),
  women: ObjectId(),
  kids: ObjectId(),
  shoes: ObjectId(),
  accessories: ObjectId(),
  archive: ObjectId(),
};

const category = (id, slug, name, description, displayOrder, active, imageUrl) => ({
  _id: id,
  slug: slug,
  name: name,
  description: description,
  parentId: null,
  imageUrl: imageUrl,
  displayOrder: NumberInt(displayOrder),
  active: active,
  createdAt: daysAgo(120),
  updatedAt: daysAgo(120),
});

db.categories.insertMany([
  category(cat.men, 'men', 'Men', 'Shirts, trousers, jackets and everyday essentials for men.', 1, true,
    '/images/catalog/category-men.svg'),
  category(cat.women, 'women', 'Women', 'Dresses, tops, knitwear and outerwear for women.', 2, true,
    '/images/catalog/category-women.svg'),
  category(cat.kids, 'kids', 'Kids', 'Comfortable clothing for children.', 3, true, null),
  category(cat.shoes, 'shoes', 'Shoes', 'Sneakers, boots and sandals.', 4, true,
    '/images/catalog/category-shoes.svg'),
  category(cat.accessories, 'accessories', 'Accessories', null, 5, true,
    '/images/catalog/category-accessories.svg'),
  category(cat.archive, 'archive', 'Archive', 'Retired collection - hidden from the storefront.', 6, false, null),
]);

const product = (p) => ({
  _id: ObjectId(),
  slug: p.slug,
  name: p.name,
  description: p.description,
  categoryId: p.categoryId,
  brand: p.brand === undefined ? null : p.brand,
  images: p.images,
  basePrice: money(p.basePrice),
  salePrice: p.salePrice ? money(p.salePrice) : null,
  active: p.active === undefined ? true : p.active,
  featured: p.featured === true,
  createdAt: daysAgo(p.ageDays),
  updatedAt: daysAgo(Math.max(0, p.ageDays - 1)),
});

const products = [
  { slug: 'men-classic-oxford-shirt', name: 'Classic Oxford Shirt', categoryId: cat.men, brand: 'Northline',
    description: 'Breathable cotton Oxford shirt with a button-down collar.', basePrice: '49.90',
    featured: true, ageDays: 2, images: img('men-classic-oxford-shirt', 'Light blue Oxford shirt on a hanger') },
  { slug: 'men-slim-chino-trousers', name: 'Slim Chino Trousers', categoryId: cat.men, brand: 'Northline',
    description: 'Slim-fit stretch chinos for everyday wear.', basePrice: '59.90', salePrice: '44.90',
    ageDays: 9, images: img('men-slim-chino-trousers', 'Beige slim chino trousers') },
  { slug: 'men-wool-blend-overcoat', name: 'Wool Blend Overcoat', categoryId: cat.men, brand: 'Atelier Grey',
    description: 'Tailored single-breasted overcoat in a warm wool blend.', basePrice: '189.00',
    ageDays: 21, images: img('men-wool-blend-overcoat', 'Charcoal wool overcoat') },
  { slug: 'men-essential-crew-tee', name: 'Essential Crew Neck T-Shirt', categoryId: cat.men, brand: null,
    description: 'Soft organic cotton crew neck tee.', basePrice: '19.90',
    ageDays: 35, images: [] },
  { slug: 'men-lightweight-performance-hooded-windbreaker-jacket-with-packable-pocket',
    name: 'Lightweight Water-Repellent Performance Hooded Windbreaker Jacket with Packable Pocket and Reflective Details',
    categoryId: cat.men, brand: 'Trailform',
    description: 'Packable windbreaker for running and travel; folds into its own chest pocket.',
    basePrice: '79.90', salePrice: '59.90', ageDays: 14,
    images: img('men-lightweight-performance-hooded-windbreaker-jacket-with-packable-pocket', 'Olive hooded windbreaker jacket') },
  { slug: 'men-retired-denim-vest', name: 'Denim Vest (Discontinued)', categoryId: cat.men, brand: 'Northline',
    description: 'Discontinued item kept for testing inactive products.', basePrice: '39.90',
    active: false, ageDays: 1, images: img('men-retired-denim-vest', 'Washed denim vest') },

  { slug: 'women-floral-midi-dress', name: 'Floral Midi Dress', categoryId: cat.women, brand: 'Maison Lila',
    description: 'Flowing midi dress with a floral print and tie waist.', basePrice: '69.90',
    featured: true, ageDays: 3, images: img('women-floral-midi-dress', 'Floral print midi dress') },
  { slug: 'women-cable-knit-sweater', name: 'Cable Knit Sweater', categoryId: cat.women, brand: 'Maison Lila',
    description: 'Chunky cable knit sweater in soft merino blend.', basePrice: '79.00', salePrice: '59.00',
    ageDays: 11, images: img('women-cable-knit-sweater', 'Cream cable knit sweater') },
  { slug: 'women-high-rise-straight-jeans', name: 'High-Rise Straight Jeans', categoryId: cat.women, brand: 'Indigo & Co',
    description: 'High-rise straight-leg jeans in rigid denim.', basePrice: '64.90',
    ageDays: 18, images: img('women-high-rise-straight-jeans', 'Mid-blue straight leg jeans') },
  { slug: 'women-silk-blend-blouse', name: 'Silk Blend Blouse', categoryId: cat.women, brand: null,
    description: 'Relaxed blouse with a subtle sheen.', basePrice: '54.90',
    ageDays: 27, images: [] },
  { slug: 'women-belted-trench-coat', name: 'Belted Trench Coat', categoryId: cat.women, brand: 'Atelier Grey',
    description: 'Classic double-breasted trench coat with belt.', basePrice: '159.00',
    featured: true, ageDays: 6, images: img('women-belted-trench-coat', 'Camel trench coat with belt') },
  { slug: 'women-ribbed-tank-top', name: 'Ribbed Tank Top', categoryId: cat.women, brand: 'Indigo & Co',
    description: 'Fitted ribbed tank top, a layering essential.', basePrice: '14.90',
    ageDays: 40, images: img('women-ribbed-tank-top', 'White ribbed tank top') },

  { slug: 'shoes-white-leather-sneakers', name: 'White Leather Sneakers', categoryId: cat.shoes, brand: 'Stride',
    description: 'Minimal low-top sneakers in smooth leather.', basePrice: '89.90',
    featured: true, ageDays: 4, images: img('shoes-white-leather-sneakers', 'Pair of white leather sneakers') },
  { slug: 'shoes-chelsea-boots', name: 'Suede Chelsea Boots', categoryId: cat.shoes, brand: 'Stride',
    description: 'Suede Chelsea boots with elastic side panels.', basePrice: '129.00', salePrice: '99.00',
    ageDays: 16, images: img('shoes-chelsea-boots', 'Brown suede Chelsea boots') },
  { slug: 'shoes-running-trainers', name: 'Cushioned Running Trainers', categoryId: cat.shoes, brand: 'Trailform',
    description: 'Lightweight trainers with responsive cushioning.', basePrice: '109.90',
    ageDays: 8, images: img('shoes-running-trainers', 'Grey running trainers') },
  { slug: 'shoes-leather-sandals', name: 'Leather Strap Sandals', categoryId: cat.shoes, brand: null,
    description: 'Flat sandals with adjustable leather straps.', basePrice: '45.00',
    ageDays: 30, images: [] },
  { slug: 'shoes-canvas-slip-ons', name: 'Canvas Slip-Ons', categoryId: cat.shoes, brand: 'Stride',
    description: 'Easy canvas slip-on shoes.', basePrice: '39.90',
    ageDays: 45, images: img('shoes-canvas-slip-ons', 'Navy canvas slip-on shoes') },

  { slug: 'acc-leather-belt', name: 'Leather Belt', categoryId: cat.accessories, brand: 'Northline',
    description: 'Full-grain leather belt with a brushed metal buckle.', basePrice: '34.90',
    ageDays: 5, images: img('acc-leather-belt', 'Brown leather belt with buckle') },
  { slug: 'acc-wool-beanie', name: 'Wool Beanie', categoryId: cat.accessories, brand: null,
    description: 'Ribbed wool beanie.', basePrice: '19.90', salePrice: '14.90',
    ageDays: 12, images: img('acc-wool-beanie', 'Grey ribbed wool beanie') },
  { slug: 'acc-canvas-tote-bag', name: 'Canvas Tote Bag', categoryId: cat.accessories, brand: 'Indigo & Co',
    description: 'Sturdy canvas tote with inner pocket.', basePrice: '24.90',
    ageDays: 20, images: [] },
  { slug: 'acc-silk-scarf', name: 'Printed Silk Scarf', categoryId: cat.accessories, brand: 'Maison Lila',
    description: 'Square silk scarf with an abstract print.', basePrice: '39.00',
    featured: true, ageDays: 7, images: img('acc-silk-scarf', 'Printed silk scarf folded') },
  { slug: 'acc-classic-sunglasses', name: 'Classic Sunglasses', categoryId: cat.accessories, brand: 'Atelier Grey',
    description: 'Acetate frame sunglasses with UV400 lenses.', basePrice: '59.00',
    ageDays: 25, images: img('acc-classic-sunglasses', 'Black acetate sunglasses') },
].map(product);

db.products.insertMany(products);

db.categories.createIndex({ slug: 1 }, { unique: true, name: 'uk_categories_slug' });
db.products.createIndex({ slug: 1 }, { unique: true, name: 'uk_products_slug' });
db.products.createIndex({ categoryId: 1, active: 1, createdAt: -1 }, { name: 'ix_products_category_active_created' });
db.products.createIndex({ featured: 1, active: 1 }, { name: 'ix_products_featured_active' });

print(`[seed] clothing_store seeded: ${db.categories.countDocuments({})} categories ` +
  `(${db.categories.countDocuments({ active: true })} active), ` +
  `${db.products.countDocuments({})} products ` +
  `(${db.products.countDocuments({ active: true })} active, ${db.products.countDocuments({ featured: true })} featured).`);
