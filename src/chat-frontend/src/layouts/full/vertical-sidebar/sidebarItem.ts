import {
  ChartPieIcon,
  CoffeeIcon,
  CpuIcon,
  FlagIcon,
  BasketIcon,
  ApertureIcon,
  LayoutGridIcon,
  BoxIcon,
  Message2Icon,
  FilesIcon,
  CalendarIcon,
  UserCircleIcon,
  ChartBarIcon,
  ShoppingCartIcon,
  ChartLineIcon,
  ChartAreaIcon,
  ChartDotsIcon,
  ChartArcsIcon,
  ChartCandleIcon,
  ChartDonut3Icon,
  ChartRadarIcon,
  LayoutIcon,
  CardboardsIcon,
  PhotoIcon,
  FileTextIcon,
  BoxAlignBottomIcon,
  BoxAlignLeftIcon,
  FileDotsIcon,
  EditCircleIcon,
  AppsIcon,
  BorderAllIcon,
  BorderHorizontalIcon,
  BorderInnerIcon,
  BorderTopIcon,
  BorderVerticalIcon,
  BorderStyle2Icon,
  LoginIcon,
  CircleDotIcon,
  UserPlusIcon,
  RotateIcon,
  ZoomCodeIcon,
  SettingsIcon,
  AlertCircleIcon,
  BrandTablerIcon,
  CodeAsterixIcon,
  BrandCodesandboxIcon,
  ColumnsIcon,
  RowInsertBottomIcon,
  EyeTableIcon,
  SortAscendingIcon,
  PageBreakIcon,
  FilterIcon,
  BoxModelIcon,
  ServerIcon,
  JumpRopeIcon,
  LayoutKanbanIcon

} from 'vue-tabler-icons';

export interface menu {
  header?: string;
  title?: string;
  icon?: any;
  to?: string;
  chip?: string;
  chipBgColor?: string;
  chipColor?: string;
  chipVariant?: string;
  chipIcon?: string;
  children?: menu[];
  disabled?: boolean;
  type?: string;
  subCaption?: string;
}

const sidebarItem: menu[] = [
  { header: 'Home' },
  {
    title: "Analytical",
    icon: ChartPieIcon,
    to: "/dashboards/analytical",
  },
  {
    title: "Classic",
    icon: CoffeeIcon,
    to: "/dashboards/classic",
  },
  {
    title: "Demographical",
    icon: CpuIcon,
    to: "/dashboards/demographical",
  },
  {
    title: "Minimal",
    icon: FlagIcon,
    to: "/dashboards/minimal",
  },
  {
    title: "eCommerce",
    icon: ShoppingCartIcon,
    to: "/dashboards/ecommerce",
  },
  {
    title: "Modern",
    icon: ApertureIcon,
    to: "/dashboards/modern",
  },
  { header: 'Apps' },
  {
    title: 'Contact',
    icon: BoxIcon,
    to: '/apps/contacts',
    chip: '2',
    chipColor: 'surface',
    chipBgColor: 'secondary'
  },

  {
    title: 'Blog',
    icon: ChartDonut3Icon,
    to: '/',
    children: [
      {
        title: 'Posts',
        icon: CircleDotIcon,
        to: '/apps/blog/posts'
      },
      {
        title: 'Detail',
        icon: CircleDotIcon,
        to: '/apps/blog/early-black-friday-amazon-deals-cheap-tvs-headphones'
      }
    ]
  },
  {
    title: 'E-Commerce',
    icon: BasketIcon,
    to: '/ecommerce/',
    children: [
      {
        title: 'Shop',
        icon: CircleDotIcon,
        to: '/ecommerce/products'
      },
      {
        title: 'Detail',
        icon: CircleDotIcon,
        to: '/ecommerce/product/detail/1'
      },
      {
        title: 'List',
        icon: CircleDotIcon,
        to: '/ecommerce/productlist'
      },
      {
        title: 'Checkout',
        icon: CircleDotIcon,
        to: '/ecommerce/checkout'
      }
    ]
  },
  {
    title: 'Chats',
    icon: Message2Icon,
    to: '/apps/chats'
  },
  {
    title: 'User Profile',
    icon: UserCircleIcon,
    to: '/',
    children: [
      {
        title: 'Profile',
        icon: CircleDotIcon,
        to: '/apps/user/profile'
      },
      {
        title: 'Followers',
        icon: CircleDotIcon,
        to: '/apps/user/profile/followers'
      },
      {
        title: 'Friends',
        icon: CircleDotIcon,
        to: '/apps/user/profile/friends'
      },
      {
        title: 'Gallery',
        icon: CircleDotIcon,
        to: '/apps/user/profile/gallery'
      }
    ]
  },
  {
    title: 'Notes',
    icon: FilesIcon,
    to: '/apps/notes'
  },
  {
    title: 'Calendar',
    icon: CalendarIcon,
    to: '/apps/calendar'
  },
  {
    title: 'Kanban',
    icon: LayoutKanbanIcon,
    to: '/apps/kanban'
  },
  { header: 'Authentication' },

  {
    title: 'Login',
    icon: LoginIcon,
    to: '#',
    children: [
      {
        title: 'Side Login',
        icon: CircleDotIcon,
        to: '/'
      },
      {
        title: 'Boxed Login',
        icon: CircleDotIcon,
        to: '/auth/login2'
      }
    ]
  },
  {
    title: 'Register',
    icon: UserPlusIcon,
    to: '#',
    children: [
      {
        title: 'Side Register',
        icon: CircleDotIcon,
        to: '/auth/register'
      },
      {
        title: 'Boxed Register',
        icon: CircleDotIcon,
        to: '/auth/register2'
      }
    ]
  },
  {
    title: 'Forgot Password',
    icon: RotateIcon,
    to: '#',
    children: [
      {
        title: 'Side Forgot Password',
        icon: CircleDotIcon,
        to: '/auth/forgot-password'
      },
      {
        title: 'Boxed Forgot Password',
        icon: CircleDotIcon,
        to: '/auth/forgot-password2'
      }
    ]
  },
  {
    title: 'Two Steps',
    icon: ZoomCodeIcon,
    to: '#',
    children: [
      {
        title: 'Side Two Steps',
        icon: SettingsIcon,
        to: '/auth/two-step'
      },
      {
        title: 'Boxed Two Steps',
        icon: SettingsIcon,
        to: '/auth/two-step2'
      }
    ]
  },

  {
    title: 'Error',
    icon: AlertCircleIcon,
    to: '/auth/404'
  },
  {
    title: 'Maintenance',
    icon: SettingsIcon,
    to: '/auth/maintenance'
  },
  { header: "Icons" },
  {
    title: "Material",
    icon: BrandCodesandboxIcon,
    to: "/icons/material",
  },
  {
    title: "Tabler",
    icon: BrandTablerIcon,
    to: "/icons/tabler",
  },

];

export default sidebarItem;
