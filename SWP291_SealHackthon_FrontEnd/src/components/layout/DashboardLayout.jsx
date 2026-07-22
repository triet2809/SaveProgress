/**
 * DashboardLayout.jsx — Khung layout chung cho mọi trang dashboard.
 * Gồm Sidebar (menu trái) + Topbar (thanh trên) + vùng nội dung <Outlet> cho route con.
 * @param {string} role - Vai trò hiện tại, quyết định menu Sidebar/Topbar hiển thị.
 */
import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import styles from './DashboardLayout.module.css';

const DashboardLayout = ({ role }) => {
  return (
    <div className={styles.layout}>
      <Sidebar role={role} />
      <div className={styles.mainWrapper}>
        <Topbar role={role} />
        <main className={styles.mainContent}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
